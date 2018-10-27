package eu.ggam.jlink.generator;

import java.nio.file.Path;

/**
 *
 * @author Guillermo González de Agüero
 */
public class WarMavenDependency extends MavenDependency {

    private Path explodedPath;

    public WarMavenDependency(MavenRepositoryHelper repoHelper, String mavenCoordinates) {
        super(repoHelper, mavenCoordinates);
    }

    public Path getExplodedPath() {
        return explodedPath;
    }

}
