package eu.ggam.jlink.generator;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Guillermo González de Agüero
 */
public class App {
    
    private static final Path TMP_DIR = Paths.get("target", "tmp").toAbsolutePath();
    
    public static void main(String... args) throws Exception {
        String m2Home = System.getenv("M2_HOME");
        if (m2Home == null) {
            throw new IllegalStateException("Set M2_HOME env variable to your local Maven repository location");
        }

        new Generator(m2Home).createImage("eu.ggam:container-test-webapp:war:1.0-SNAPSHOT", TMP_DIR);
    }

}
