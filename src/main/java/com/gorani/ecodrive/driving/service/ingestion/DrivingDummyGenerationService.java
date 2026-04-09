package com.gorani.ecodrive.driving.service.ingestion;

import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class DrivingDummyGenerationService {

    private static final DateTimeFormatter RUN_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int SESSION_COUNT_PER_RUN = 1;

    private final String pythonCommand;
    private final Path scriptPath;
    private final List<GenerationTarget> targets;
    private final JdbcTemplate jdbcTemplate;

    public DrivingDummyGenerationService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.driving-dummy.python-command:python3}") String pythonCommand,
            @Value("${app.driving-dummy.script-path:scripts/generate_driving_dummy_json.py}") String scriptPath,
            @Value("${app.driving-dummy.target-users:1:1}") String targetUsers
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.pythonCommand = pythonCommand;
        this.scriptPath = Path.of(scriptPath).normalize();
        this.targets = parseTargets(targetUsers);
    }

    public DummyDrivingGenerationResult generateTodayBatches(Path outputDir) {
        LocalDateTime runAt = LocalDateTime.now();
        List<String> generatedFiles = new ArrayList<>();
        Path backendRoot = resolveBackendRoot();
        Path resolvedScriptPath = resolveScriptPath(backendRoot);
        Path resolvedOutputDir = outputDir.isAbsolute()
                ? outputDir.normalize()
                : backendRoot.resolve(outputDir).normalize();

        for (GenerationTarget target : targets) {
            VehicleGenerationProfile vehicleProfile = loadVehicleGenerationProfile(target.userVehicleId());
            List<String> command = new ArrayList<>(List.of(
                    pythonCommand,
                    resolvedScriptPath.toString(),
                    "--user-id", String.valueOf(target.userId()),
                    "--user-vehicle-id", String.valueOf(target.userVehicleId()),
                    "--run-at", runAt.format(RUN_AT_FORMAT),
                    "--output-dir", resolvedOutputDir.toString()
            ));
            if (vehicleProfile != null) {
                command.add("--fuel-type");
                command.add(vehicleProfile.fuelType());
                command.add("--body-type");
                command.add(vehicleProfile.bodyType());
            }

            try {
                String generatedPath = execute(command);
                generatedFiles.add(generatedPath);
                log.info("주행 더미데이터 생성 완료. userId={}, vehicleId={}, fuelType={}, bodyType={}, sessions={}, file={}",
                        target.userId(),
                        target.userVehicleId(),
                        vehicleProfile == null ? "UNKNOWN" : vehicleProfile.fuelType(),
                        vehicleProfile == null ? "UNKNOWN" : vehicleProfile.bodyType(),
                        SESSION_COUNT_PER_RUN,
                        generatedPath);
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(
                        "주행 더미데이터 생성 스크립트 실행에 실패했습니다. userId=" + target.userId(),
                        e
                );
            }
        }

        return new DummyDrivingGenerationResult(
                generatedFiles.size(),
                targets.size(),
                generatedFiles
        );
    }

    private Path resolveScriptPath(Path backendRoot) {
        if (scriptPath.isAbsolute() && Files.exists(scriptPath)) {
            return scriptPath;
        }

        Path normalizedRelative = stripLeadingParentSegments(scriptPath);
        Path backendResolved = backendRoot.resolve(normalizedRelative).normalize();
        if (Files.exists(backendResolved)) {
            return backendResolved;
        }

        throw new IllegalStateException("주행 더미데이터 스크립트를 찾을 수 없습니다. configured=" + scriptPath);
    }

    private Path stripLeadingParentSegments(Path path) {
        Path normalized = path.normalize();
        Path result = Path.of("");
        for (Path segment : normalized) {
            if ("..".equals(segment.toString())) {
                continue;
            }
            result = result.resolve(segment);
        }
        return result;
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

    private VehicleGenerationProfile loadVehicleGenerationProfile(long userVehicleId) {
        return jdbcTemplate.query("""
                        select vm.fuel_type, vm.body_type
                        from user_vehicles uv
                        join vehicle_models vm on uv.vehicle_model_id = vm.id
                        where uv.id = ?
                        """,
                rs -> rs.next()
                        ? new VehicleGenerationProfile(
                        normalizeFuelType(rs.getString("fuel_type")),
                        normalizeBodyType(rs.getString("body_type"))
                )
                        : null,
                userVehicleId
        );
    }

    private String normalizeFuelType(String fuelType) {
        if (fuelType == null || fuelType.isBlank()) {
            return "GASOLINE";
        }

        return switch (fuelType.trim().toUpperCase(Locale.ROOT)) {
            case "DIESEL" -> "DIESEL";
            case "HYBRID" -> "HYBRID";
            default -> "GASOLINE";
        };
    }

    private String normalizeBodyType(String bodyType) {
        if (bodyType == null || bodyType.isBlank()) {
            return "MIDSIZE";
        }

        String normalized = bodyType.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("SMALL") || containsBodyKeyword(bodyType, "경", "소형", "COMPACT")) {
            return "SMALL";
        }
        if (normalized.contains("LARGE") || containsBodyKeyword(bodyType, "대형", "SUV", "승합")) {
            return "LARGE";
        }
        return "MIDSIZE";
    }

    private boolean containsBodyKeyword(String bodyType, String... keywords) {
        String normalized = bodyType.toUpperCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String execute(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().reduce("", (acc, line) -> acc.isEmpty() ? line : acc + "\n" + line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Python 스크립트 종료 코드가 비정상입니다. exitCode=" + exitCode + ", output=" + output);
        }

        return output;
    }

    private List<GenerationTarget> parseTargets(String targetUsers) {
        return List.of(targetUsers.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> {
                    String[] tokens = value.split(":");
                    if (tokens.length != 2) {
                        throw new IllegalArgumentException("target-users 형식이 잘못되었습니다. expected=userId:userVehicleId, actual=" + value);
                    }
                    return new GenerationTarget(
                            Long.parseLong(tokens[0].trim()),
                            Long.parseLong(tokens[1].trim())
                    );
                })
                .toList();
    }

    private record GenerationTarget(
            long userId,
            long userVehicleId
    ) {
    }

    private record VehicleGenerationProfile(
            String fuelType,
            String bodyType
    ) {
    }
}
