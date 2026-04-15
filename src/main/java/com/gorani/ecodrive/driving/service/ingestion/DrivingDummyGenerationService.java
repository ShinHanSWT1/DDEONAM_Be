package com.gorani.ecodrive.driving.service.ingestion;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingGenerationResult;
import com.gorani.ecodrive.user.repository.UserRepository;
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
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public DrivingDummyGenerationService(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            @Value("${app.driving-dummy.python-command:python3}") String pythonCommand,
            @Value("${app.driving-dummy.script-path:scripts/generate_driving_dummy_json.py}") String scriptPath
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.pythonCommand = pythonCommand;
        this.scriptPath = Path.of(scriptPath).normalize();
    }

    public DummyDrivingGenerationResult generateTodayBatches(Path outputDir) {
        List<GenerationTarget> targets = resolveAllActiveTargets();
        log.info("Resolved generation targets for all users. count={}", targets.size());
        return generateTodayBatches(outputDir, targets);
    }

    public DummyDrivingGenerationResult generateTodayBatchesForUser(Long userId, Path outputDir) {
        validateUserExists(userId);
        GenerationTarget target = resolveActiveTargetForUser(userId);
        log.info("Resolved generation target for user. userId={}, userVehicleId={}", target.userId(), target.userVehicleId());
        return generateTodayBatches(outputDir, List.of(target));
    }

    public DummyDrivingGenerationResult generateTodayBatchesForUserVehicle(Long userId, Long userVehicleId, Path outputDir) {
        validateUserExists(userId);
        GenerationTarget target = resolveTargetForUserVehicle(userId, userVehicleId);
        log.info("Resolved generation target for user vehicle. userId={}, userVehicleId={}", target.userId(), target.userVehicleId());
        return generateTodayBatches(outputDir, List.of(target));
    }

    private DummyDrivingGenerationResult generateTodayBatches(Path outputDir, List<GenerationTarget> targets) {
        LocalDateTime runAt = LocalDateTime.now();
        List<String> generatedFiles = new ArrayList<>();
        Path resolvedScriptPath = resolveScriptPath();
        Path resolvedOutputDir = resolveOutputDir(outputDir);

        log.info(
                "Starting driving dummy generation. runAt={}, targetCount={}, scriptPath={}, outputDir={}",
                runAt,
                targets.size(),
                resolvedScriptPath,
                resolvedOutputDir
        );

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
                log.info(
                        "Executing driving dummy generator command. userId={}, userVehicleId={}, fuelType={}, bodyType={}",
                        target.userId(),
                        target.userVehicleId(),
                        vehicleProfile == null ? "UNKNOWN" : vehicleProfile.fuelType(),
                        vehicleProfile == null ? "UNKNOWN" : vehicleProfile.bodyType()
                );

                String generatedPath = execute(command);
                generatedFiles.add(generatedPath);
                log.info(
                        "Driving dummy generated. userId={}, vehicleId={}, fuelType={}, bodyType={}, sessions={}, file={}",
                        target.userId(),
                        target.userVehicleId(),
                        vehicleProfile == null ? "UNKNOWN" : vehicleProfile.fuelType(),
                        vehicleProfile == null ? "UNKNOWN" : vehicleProfile.bodyType(),
                        SESSION_COUNT_PER_RUN,
                        generatedPath
                );
            } catch (IOException | InterruptedException e) {
                log.error(
                        "Driving dummy generation script execution failed. userId={}, userVehicleId={}, command={}",
                        target.userId(),
                        target.userVehicleId(),
                        command,
                        e
                );
                throw new IllegalStateException(
                        "Driving dummy generation script execution failed. userId=" + target.userId(),
                        e
                );
            }
        }

        log.info("Driving dummy generation finished. generatedBatches={}, attemptedUsers={}, generatedFiles={}",
                generatedFiles.size(), targets.size(), generatedFiles);

        return new DummyDrivingGenerationResult(
                generatedFiles.size(),
                targets.size(),
                generatedFiles
        );
    }

    private List<GenerationTarget> resolveAllActiveTargets() {
        List<GenerationTarget> targets = jdbcTemplate.query("""
                        select uv.user_id, uv.id as user_vehicle_id
                        from user_vehicles uv
                        where uv.status = 'ACTIVE'
                        order by uv.user_id asc, uv.registered_at asc, uv.id asc
                        """,
                (rs, rowNum) -> new GenerationTarget(
                        rs.getLong("user_id"),
                        rs.getLong("user_vehicle_id")
                )
        );

        if (targets.isEmpty()) {
            log.warn("No active vehicle target found for driving dummy generation.");
            throw new CustomException(ErrorCode.NO_ACTIVE_VEHICLE);
        }
        return targets;
    }

    private GenerationTarget resolveActiveTargetForUser(Long userId) {
        GenerationTarget target = jdbcTemplate.query("""
                        select uv.user_id, uv.id as user_vehicle_id
                        from user_vehicles uv
                        join users u on u.id = uv.user_id
                        where uv.user_id = ?
                          and uv.status = 'ACTIVE'
                        order by case when uv.id = u.representative_user_vehicle_id then 0 else 1 end,
                                 uv.registered_at desc,
                                 uv.id desc
                        limit 1
                        """,
                rs -> rs.next()
                        ? new GenerationTarget(
                        rs.getLong("user_id"),
                        rs.getLong("user_vehicle_id")
                )
                        : null,
                userId
        );

        if (target == null) {
            log.warn("No active vehicle target found for user. userId={}", userId);
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return target;
    }

    private GenerationTarget resolveTargetForUserVehicle(Long userId, Long userVehicleId) {
        GenerationTarget target = jdbcTemplate.query("""
                        select uv.user_id, uv.id as user_vehicle_id
                        from user_vehicles uv
                        where uv.user_id = ?
                          and uv.id = ?
                          and uv.status = 'ACTIVE'
                        limit 1
                        """,
                rs -> rs.next()
                        ? new GenerationTarget(
                        rs.getLong("user_id"),
                        rs.getLong("user_vehicle_id")
                )
                        : null,
                userId,
                userVehicleId
        );

        if (target == null) {
            log.warn("No active vehicle target found for user vehicle. userId={}, userVehicleId={}", userId, userVehicleId);
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return target;
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            log.warn("Driving dummy generation requested for non-existing user. userId={}", userId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Path resolveScriptPath() {
        if (scriptPath.isAbsolute() && Files.exists(scriptPath)) {
            return scriptPath.normalize();
        }

        Path backendRoot = resolveBackendRoot();
        Path normalizedRelative = stripLeadingParentSegments(scriptPath);
        Path backendResolved = backendRoot.resolve(normalizedRelative).normalize();
        if (Files.exists(backendResolved)) {
            return backendResolved;
        }

        throw new IllegalStateException("Driving dummy script not found. configured=" + scriptPath);
    }

    private Path resolveOutputDir(Path outputDir) {
        if (outputDir.isAbsolute()) {
            return outputDir.normalize();
        }

        return resolveBackendRoot().resolve(outputDir).normalize();
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

    private Path  resolveBackendRoot() {
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

        throw new IllegalStateException("EcoDrive_be module root not found. user.dir=" + current);
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
        if (normalized.contains("SMALL") || containsBodyKeyword(bodyType, "경차", "소형", "COMPACT")) {
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
            log.error("Driving dummy python script exited with non-zero status. exitCode={}, output={}", exitCode, output);
            throw new IllegalStateException("Python script exited with non-zero status. exitCode=" + exitCode + ", output=" + output);
        }

        log.debug("Driving dummy python script completed. output={}", output);
        return output;
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
