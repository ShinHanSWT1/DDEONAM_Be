package com.gorani.ecodrive.driving.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class DrivingDummyFileManager {

    private static final DateTimeFormatter FAILED_SUFFIX_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Path baseDir;

    public DrivingDummyFileManager(
            @Value("${app.driving-dummy.base-dir:generated/driving}") String baseDir
    ) {
        Path configuredPath = Path.of(baseDir).normalize();
        this.baseDir = configuredPath.isAbsolute()
                ? configuredPath
                : resolveBackendRoot().resolve(configuredPath).normalize();
    }

    @PostConstruct
    void initializeDirectories() throws IOException {
        Files.createDirectories(getPendingDir());
        Files.createDirectories(getFailedDir());
    }

    public List<Path> listPendingJsonFiles() throws IOException {
        try (Stream<Path> stream = Files.list(getPendingDir())) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    public void deleteProcessed(Path path) throws IOException {
        Files.deleteIfExists(path);
    }

    public Path moveToFailed(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        String failedName = fileName.replace(
                ".json",
                "-" + LocalDateTime.now().format(FAILED_SUFFIX_FORMAT) + ".json"
        );
        Path failedPath = getFailedDir().resolve(failedName);
        return Files.move(path, failedPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path getPendingDir() {
        return baseDir.resolve("pending");
    }

    public Path getFailedDir() {
        return baseDir.resolve("failed");
    }

    private Path resolveBackendRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

        if (Files.exists(current.resolve("build.gradle"))) {
            return current;
        }

        Path nestedBackendRoot = current.resolve("EcoDrive_be");
        if (Files.exists(nestedBackendRoot.resolve("build.gradle"))) {
            return nestedBackendRoot;
        }

        Path walker = current;
        while (walker != null) {
            if (Files.exists(walker.resolve("build.gradle"))) {
                return walker;
            }

            Path nested = walker.resolve("EcoDrive_be");
            if (Files.exists(nested.resolve("build.gradle"))) {
                return nested;
            }

            walker = walker.getParent();
        }

        throw new IllegalStateException("EcoDrive_be 모듈 루트를 찾을 수 없습니다. user.dir=" + current);
    }
}
