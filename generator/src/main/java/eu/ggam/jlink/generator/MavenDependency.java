package eu.ggam.jlink.generator;

import java.io.File;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import static java.util.stream.Collectors.toSet;
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
        Set<ModuleReference> modules = new HashSet<>();
        for (Path jarFile : jarFiles) {
            if (!Files.isDirectory(jarFile) && !jarFile.getFileName().toString().endsWith(".jar")) {
                throw new IllegalArgumentException("Not a JAR");
            }

            modules.addAll(ModuleFinder.of(jarFiles.toArray(new Path[jarFiles.size()])).
                    findAll());
        }

        return modules;
    }

    public Set<Path> getJarLocations() {
        return new HashSet<>(jarLocations);
    }

    public Set<ModuleReference>getModuleReferences() {
        return new HashSet<>(moduleReferences);
    }
}
