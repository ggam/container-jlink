package eu.ggam.jlink.generator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

/**
 *
 * @author Guillermo González de Agüero
 */
public class WarTransformer {

    private final MavenRepositoryHelper repoHelper;

    private static final Path EXPLODED_WAR_PATH = Paths.get("target", "tmp", "exploded-war");

    public WarTransformer(MavenRepositoryHelper repoHelper) {
        this.repoHelper = repoHelper;
    }

    public Path transformWar(String warMavenCoords) throws IOException {
        Files.createDirectories(EXPLODED_WAR_PATH);

        Path warPath = repoHelper.getLocalPath(warMavenCoords);

        try (var file = new ZipFile(warPath.toFile())) {
            file.stream().forEach(e -> {
                try {
                    Path newFilePath;
                    if (e.getName().startsWith("WEB-INF/classes/")) {
                        newFilePath = EXPLODED_WAR_PATH.resolve(e.getName().replace("WEB-INF/classes/", ""));
                    } else {
                         // Move everything outside WEB-INF/classes under _ROOT_
                        newFilePath = EXPLODED_WAR_PATH.resolve("_ROOT_").resolve(e.getName());
                    }

                    if (e.isDirectory()) {
                        Files.createDirectories(newFilePath);
                    } else {
                        try (var is = file.getInputStream(e);
                                var bis = new BufferedInputStream(is);
                                var os = Files.newOutputStream(newFilePath);
                                var bos = new BufferedOutputStream(os)) {
                            bis.transferTo(bos);
                        }
                    }
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        }

        return EXPLODED_WAR_PATH;
    }

}
