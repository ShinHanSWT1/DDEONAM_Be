package com.gorani.ecodrive.driving.service;

import com.gorani.ecodrive.driving.dto.DummyDrivingGenerationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class DrivingDummyGenerationService {

    private static final DateTimeFormatter RUN_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final String pythonCommand;
    private final Path scriptPath;
    private final int defaultSessionsPerUser;
    private final double extraSessionProbability;
    private final List<GenerationTarget> targets;

    public DrivingDummyGenerationService(
            @Value("${app.driving-dummy.python-command:python3}") String pythonCommand,
            @Value("${app.driving-dummy.script-path:scripts/generate_driving_dummy_json.py}") String scriptPath,
            @Value("${app.driving-dummy.default-sessions-per-user:2}") int defaultSessionsPerUser,
            @Value("${app.driving-dummy.extra-session-probability:0.2}") double extraSessionProbability,
            @Value("${app.driving-dummy.target-users:1:1}") String targetUsers
    ) {
        this.pythonCommand = pythonCommand;
        this.scriptPath = Path.of(scriptPath).normalize();
        this.defaultSessionsPerUser = defaultSessionsPerUser;
        this.extraSessionProbability = extraSessionProbability;
        this.targets = parseTargets(targetUsers);
    }

    public DummyDrivingGenerationResult generateTodayBatches(Path outputDir) {
        LocalDateTime runAt = LocalDateTime.now();
        List<String> generatedFiles = new ArrayList<>();
        Path resolvedScriptPath = resolveScriptPath(outputDir);

        for (GenerationTarget target : targets) {
            int sessionCount = defaultSessionsPerUser + (shouldAddExtraSession() ? 1 : 0);
            List<String> command = List.of(
                    pythonCommand,
                    resolvedScriptPath.toString(),
                    "--user-id", String.valueOf(target.userId()),
                    "--user-vehicle-id", String.valueOf(target.userVehicleId()),
                    "--sessions", String.valueOf(sessionCount),
                    "--run-at", runAt.format(RUN_AT_FORMAT),
                    "--output-dir", outputDir.toString()
            );

            try {
                String generatedPath = execute(command);
                generatedFiles.add(generatedPath);
                log.info("주행 더미데이터 생성 완료. userId={}, vehicleId={}, sessions={}, file={}",
                        target.userId(), target.userVehicleId(), sessionCount, generatedPath);
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

    private Path resolveScriptPath(Path outputDir) {
        if (scriptPath.isAbsolute() && Files.exists(scriptPath)) {
            return scriptPath;
        }

        Path cwdResolved = Path.of(System.getProperty("user.dir"))
                .resolve(scriptPath)
                .normalize();
        if (Files.exists(cwdResolved)) {
            return cwdResolved;
        }

        Path projectRoot = inferProjectRoot(outputDir);
        Path normalizedRelative = stripLeadingParentSegments(scriptPath);
        Path projectResolved = projectRoot.resolve(normalizedRelative).normalize();
        if (Files.exists(projectResolved)) {
            return projectResolved;
        }

        throw new IllegalStateException("주행 더미데이터 스크립트를 찾을 수 없습니다. configured=" + scriptPath);
    }

    private Path inferProjectRoot(Path outputDir) {
        Path current = outputDir.toAbsolutePath().normalize();
        for (int i = 0; i < 3 && current.getParent() != null; i++) {
            current = current.getParent();
        }
        return current;
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

    private boolean shouldAddExtraSession() {
        return ThreadLocalRandom.current().nextDouble() < extraSessionProbability;
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
}
