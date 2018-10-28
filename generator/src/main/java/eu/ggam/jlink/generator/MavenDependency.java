package eu.ggam.jlink.generator;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import java.util.stream.Stream;
import org.eclipse.aether.artifact.Artifact;

/**
 *
 * @author Guillermo González de Agüero
 */
public class MavenDependency {

    private final Set<Path> jarLocations;
    private final Set<ModuleReference> moduleReferences;

    public MavenDependency(MavenRepositoryHelper repoHelper, String mavenCoordinates) {
        jarLocations = repoHelper.getDependencies(mavenCoordinates).
                map(Artifact::getFile).
                map(File::toPath).
                collect(toSet());
        moduleReferences = getModuleDependenciesForJars(jarLocations);
    }

    public MavenDependency(MavenRepositoryHelper repoHelper, String mavenCoordinates, Path transformedWarPath) {
        // Get WAR dependencies excluding the WAR itself
        jarLocations = repoHelper.getDependencies(mavenCoordinates).
                filter(a -> !mavenCoordinates.startsWith(a.getGroupId() + ":" + a.getArtifactId() + ":" + a.getExtension())).
                map(Artifact::getFile).
                map(File::toPath).
                collect(toSet());

        // Add the exploded and transformed WAR to the list
        jarLocations.add(transformedWarPath);
        moduleReferences = getModuleDependenciesForJars(jarLocations);
    }

    private Set<ModuleReference> getModuleDependenciesForJars(Set<Path> jarFiles) {
        // Assert all files are JARs
        jarFiles.stream().
                filter(p -> !Files.isDirectory(p)).
                filter(p -> p.endsWith(".jar")).
                findAny().
                ifPresent(p -> {
                    throw new IllegalArgumentException(p + " is not a JAR");
                });

        // Create a module finder containing the JRE modules
        ModuleFinder moduleFinder = ModuleFinder.compose(
                ModuleFinder.ofSystem(), ModuleFinder.of(jarFiles.toArray(new Path[jarFiles.size()])));

        String modulePath = jarFiles.stream().
                map(Path::toString).
                collect(joining(":"));

        PrintWriter stderr = new PrintWriter(new OutputStreamWriter(System.err));

        ToolProvider jdepsTool = ToolProvider.findFirst("jdeps").get();

        Set<ModuleReference> modules = new HashSet<>();

        StringWriter sw = new StringWriter();
        PrintWriter stdout = new PrintWriter(sw);
        for (Path jarFile : jarFiles) {
            jdepsTool.run(stdout, stderr, new String[]{
                "--module-path", modulePath,
                "--print-module-deps",
                jarFile.toString()});

            // Get the module reference for the current file
            modules.add(ModuleFinder.of(jarFile).
                    findAll().
                    iterator().
                    next());
        }

        String output = sw.toString();
        output = output.substring(0, output.length() - 1); // Remove trailing end of line
        output = output.replaceAll("\\n", ",");

        // Get all missing dependend (JRE) modules
        Stream.of(output.split(",")).
                map(moduleFinder::find).
                map(Optional::get).
                collect(Collectors.toCollection(() -> modules));
        
        return modules;
    }

    public Set<Path> getJarLocations() {
        return new HashSet<>(jarLocations);
    }

    public Set<ModuleReference> getModuleReferences() {
        return new HashSet<>(moduleReferences);
    }
}
