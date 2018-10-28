package eu.ggam.jlink.generator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.spi.ToolProvider;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author Guillermo González de Agüero
 */
public class Generator {

    private static final Logger LOGGER = Logger.getLogger(Generator.class.getName());

    private final String m2Home;

    public Generator(String m2Home) {
        this.m2Home = Objects.requireNonNull(m2Home);
    }

    public void createImage(String warCoordinates, Path temporaryDirectory) throws Exception {
        if (Files.exists(temporaryDirectory)) {
            Files.walk(temporaryDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectory(temporaryDirectory);

        MavenRepositoryHelper repoHelper = new MavenRepositoryHelper(m2Home);

        Path transformedWar = new WarTransformer(repoHelper, temporaryDirectory).transformWar(warCoordinates);

        MavenDependency serverDependency = new MavenDependency(repoHelper, "eu.ggam:container-impl:1.0-SNAPSHOT");
        MavenDependency launcherDependency = new MavenDependency(repoHelper, "eu.ggam.jlink:launcher:1.0-SNAPSHOT");
        MavenDependency appDependency = new MavenDependency(repoHelper, warCoordinates, transformedWar);

        // Remove libraries from transformed WAR
        Files.walk(transformedWar.resolve(Paths.get("_ROOT_", "WEB-INF", "lib"))).
                map(Path::toFile).
                forEach(File::delete);

        LOGGER.log(Level.INFO,
                "--------------\n"
                + "App modules: {0}\n"
                + "Server modules: {1}\n"
                + "Launcher modules: {2}\n",
                new Object[]{
                    moduleReferencesToString(appDependency.getModuleReferences()),
                    moduleReferencesToString(serverDependency.getModuleReferences()),
                    moduleReferencesToString(launcherDependency.getModuleReferences())});

        AggregatedImage aggregatedImage = new AggregatedImage();
        aggregatedImage.addToBaseLayer(appDependency);
        aggregatedImage.addToBaseLayer(launcherDependency);
        aggregatedImage.addToServerLayer(serverDependency);

        LOGGER.log(Level.INFO,
                "--------------\n"
                + "Aggregated boot layer: {0}\n"
                + "Aggregated server layer: {1}\n",
                new Object[]{
                    moduleReferencesToString(aggregatedImage.getBaseModules()),
                    moduleReferencesToString(aggregatedImage.getServerModules())});

        Path imageDestination = temporaryDirectory.resolve("jlink-output");
        String[] jlinkArgs = new String[]{
            "--verbose",
            "--module-path", aggregatedImage.getJlinkModulePath(),
            "--add-modules", moduleReferencesToString(aggregatedImage.getBaseModules()),
            "--compress", "2",
            "--vm=server",
            "--output", imageDestination.toString()
        };

        LOGGER.log(Level.INFO, "--------------\n"
                + "Invoking Jlink with arguments: {0}\n", new Object[]{Arrays.toString(jlinkArgs)});

        LOGGER.info("--------------");
        ToolProvider jlink = ToolProvider.findFirst("jlink").get();
        jlink.run(System.out, System.err, jlinkArgs);

        // Copy server libraries
        Path server = Files.createDirectories(imageDestination.resolve(Paths.get("lib", "runtime-impl")));
        aggregatedImage.getServerModules().
                stream().
                map(ModuleReference::location).
                map(Optional::get).
                map(Paths::get).
                forEach(p -> {
                    try {
                        Files.copy(p, server.resolve(p.getFileName()));
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });

        // Replace launcher script
        Path launcherFile = imageDestination.resolve(Paths.get("bin", "launch"));
        Files.copy(
                Paths.get(getClass().getResource("/jlink/bin/launch").toURI()),
                launcherFile);

        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(launcherFile);
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(launcherFile, permissions);

        // Add server specific files
        // TODO: bundle the server as a JMOD archive
        Files.copy(
                Paths.get(getClass().getResource("/jlink/conf/app.properties").toURI()),
                imageDestination.resolve(Paths.get("conf", "app.properties")));

        Files.copy(
                Paths.get(getClass().getResource("/jlink/conf/logging.properties").toURI()),
                imageDestination.resolve(Paths.get("conf", "logging.properties")),
                StandardCopyOption.REPLACE_EXISTING);

        LOGGER.log(Level.INFO, "--------------\n"
                + "Image generated at {0}!", new Object[]{imageDestination});
    }

    private String moduleReferencesToString(Set<ModuleReference> modules) {
        return modules.stream().
                map(ModuleReference::descriptor).
                map(ModuleDescriptor::name).
                collect(joining(","));
    }

}
