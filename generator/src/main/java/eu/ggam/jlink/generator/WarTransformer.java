package eu.ggam.jlink.generator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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

    public Path transFormWar(String warMavenCoords) throws IOException {
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

                    System.out.println("- Copying " + e.getName() + " to " + newFilePath.toAbsolutePath());
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

    private static class WarTransformerFileWalker extends SimpleFileVisitor<Path> {

        private final Path patchedWar;
        private final Path patchedWarRoot;

        public WarTransformerFileWalker(Path patchedWarDestination) {
            this.patchedWar = patchedWarDestination;
            this.patchedWarRoot = patchedWarDestination.resolve("_ROOT_");
        }

        @Override
        public FileVisitResult visitFile(Path file,
                BasicFileAttributes attributes) {

            try {
                Path targetFile = patchedWar.resolve(patchedWarRoot.resolve(Paths.get("WEB-INF", "classes")).relativize(file));
                if (!Files.exists(targetFile)) {
                    Files.move(file, targetFile);
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir,
                BasicFileAttributes attributes) {
            if (EXPLODED_WAR_PATH.equals(dir)) {
                return FileVisitResult.CONTINUE;
            }

            try {
                Path newDir = patchedWar.resolve(patchedWarRoot.resolve(Paths.get("WEB-INF", "classes")).relativize(dir));
                if (!Files.exists(newDir)) {
                    Files.createDirectories(newDir);
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }

            return FileVisitResult.CONTINUE;
        }
    }
}
