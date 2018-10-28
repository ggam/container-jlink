package eu.ggam.jlink.generator;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author Guillermo González de Agüero
 */
public class App {

    private static final Path TMP_DIR = Paths.get("target", "tmp").toAbsolutePath();

    private final String m2Home;

    public App(String m2Home) {
        this.m2Home = m2Home;
    }

    public static void main(String... args) throws Exception {
        if (Files.exists(TMP_DIR)) {
            Files.walk(TMP_DIR)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        Files.createDirectory(TMP_DIR);

        new App("/home/guillermo/.m2").createImage("eu.ggam:container-test-webapp:war:1.0-SNAPSHOT", TMP_DIR.resolve("jlink-output-test"));
    }

    public void createImage(String warCoordinates, Path imageDestination) throws Exception {
        MavenRepositoryHelper repoHelper = new MavenRepositoryHelper(m2Home);

        Path transformedWar = new WarTransformer(repoHelper).transformWar(warCoordinates);

        MavenDependency serverDependency = new MavenDependency(repoHelper, "eu.ggam:container-impl:1.0-SNAPSHOT");
        MavenDependency launcherDependency = new MavenDependency(repoHelper, "eu.ggam.jlink:launcher:1.0-SNAPSHOT");
        MavenDependency appDependency = new MavenDependency(repoHelper, warCoordinates, transformedWar);

        // Remove libraries from transformed WAR
        Files.walk(transformedWar.resolve(Paths.get("_ROOT_", "WEB-INF", "lib")))
                .filter(p -> !transformedWar.equals(p))
                .peek(p -> System.out.println("Deleting library from WAR: " + p.getFileName()))
                .map(Path::toFile)
                .forEach(File::delete);

        System.out.println("--------------");
        System.out.println("App modules: " + moduleReferencesToString(appDependency.getModuleReferences()));
        System.out.println("Server modules: " + moduleReferencesToString(serverDependency.getModuleReferences()));
        System.out.println("Launcher modules: " + moduleReferencesToString(launcherDependency.getModuleReferences()));

        AggregatedImage aggregatedImage = new AggregatedImage();
        aggregatedImage.addToBaseLayer(appDependency);
        aggregatedImage.addToBaseLayer(launcherDependency);
        aggregatedImage.addToServerLayer(serverDependency);

        System.out.println("--------------");
        System.out.println("Aggregated boot layer: " + moduleReferencesToString(aggregatedImage.getBaseModules()));
        System.out.println("Aggregated server layer: " + moduleReferencesToString(aggregatedImage.getServerModules()));

        String[] jlinkArgs = new String[]{
            "--verbose",
            "--module-path", aggregatedImage.getJlinkModulePath(),
            "--add-modules", moduleReferencesToString(aggregatedImage.getBaseModules()),
            "--compress", "2",
            "--vm=server",
            "--output", imageDestination.toString()
        };

        System.out.println("--------------");
        System.out.println("Invoking Jlink with arguments: " + Arrays.toString(jlinkArgs));
        ToolProvider jlink = ToolProvider.findFirst("jlink").get();
        System.out.println("--------------");
        jlink.run(System.out, System.err, jlinkArgs);

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

        // Replace launcher
        Files.copy(
                Paths.get("src", "main", "jlink", "bin", "launch"),
                imageDestination.resolve(Paths.get("bin", "launch")));
    }
    
    private String moduleReferencesToString(Set<ModuleReference> modules) {
        return modules.stream().
                map(ModuleReference::descriptor).
                map(ModuleDescriptor::name).
                collect(joining(","));
    }

}
