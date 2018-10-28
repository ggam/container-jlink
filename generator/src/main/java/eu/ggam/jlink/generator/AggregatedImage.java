package eu.ggam.jlink.generator;

import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 *
 * @author Guillermo González de Agüero
 */
public class AggregatedImage {

    private final Set<Path> modulePath = new HashSet<>();

    private final Set<ModuleReference> baseModules = new HashSet<>();
    private final Set<ModuleReference> serverModules = new HashSet<>();

    public void addToBaseLayer(MavenDependency mavenDependency) {
        baseModules.addAll(mavenDependency.getModuleReferences());
        modulePath.addAll(mavenDependency.getJarLocations());
    }

    public void addToServerLayer(MavenDependency mavenDependency) {
        // Add JRE modules to the base layer
        baseModules.addAll(
                mavenDependency.getModuleReferences().
                        stream().
                        filter(this::isJreModule).
                        collect(toSet()));

        serverModules.addAll(mavenDependency.getModuleReferences().
                stream().
                filter(m -> !isJreModule(m)).
                collect(toSet()));

        modulePath.addAll(mavenDependency.getJarLocations());
    }

    private boolean isJreModule(ModuleReference module) {
        String name = module.descriptor().name();
        return name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.");
    }

    public Set<ModuleReference> getBaseModules() {
        return new HashSet<>(baseModules);
    }

    public Set<ModuleReference> getServerModules() {
        return new HashSet<>(serverModules);
    }

    public String getJlinkModulePath() {
        return modulePath.stream().
                map(Path::toAbsolutePath).
                map(Path::toString).
                collect(joining(":"));
    }
}
