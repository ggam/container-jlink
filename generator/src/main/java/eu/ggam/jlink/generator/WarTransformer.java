package eu.ggam.jlink.generator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 *
 * @author Guillermo González de Agüero
 */
public class WarTransformer {

    private final MavenRepositoryHelper repoHelper;
    private final Path explodedWarPath;

    public WarTransformer(MavenRepositoryHelper repoHelper, Path tempDirectory) {
        this.repoHelper = repoHelper;
        this.explodedWarPath = tempDirectory.resolve("exploded-war");
    }

    public Path transformWar(String warMavenCoords) throws IOException {
        Files.createDirectories(explodedWarPath);

        Path warPath = repoHelper.getLocalPath(warMavenCoords);

        try (var file = new ZipFile(warPath.toFile())) {
            file.stream().forEach(e -> {
                try {
                    Path newFilePath;
                    if (e.getName().startsWith("WEB-INF/classes/")) {
                        newFilePath = explodedWarPath.resolve(e.getName().replace("WEB-INF/classes/", ""));
                    } else {
                         // Move everything outside WEB-INF/classes under _ROOT_
                        newFilePath = explodedWarPath.resolve("_ROOT_").resolve(e.getName());
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

        return explodedWarPath;
    }

}
