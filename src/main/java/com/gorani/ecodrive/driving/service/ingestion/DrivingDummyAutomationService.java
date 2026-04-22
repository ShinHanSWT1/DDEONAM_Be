package com.gorani.ecodrive.driving.service.ingestion;

import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingAutomationResult;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingGenerationResult;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingRefreshResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrivingDummyAutomationService {

    private final DrivingDummyGenerationService generationService;
    private final DrivingDummyRefreshService refreshService;
    private final DrivingDummyFileManager fileManager;

    public DummyDrivingAutomationResult generateAndRefreshForAllUsers() {
        log.info("Driving dummy automation started for all active users. pendingDir={}", fileManager.getPendingDir());
        DummyDrivingGenerationResult generation = generationService.generateTodayBatches(fileManager.getPendingDir());
        DummyDrivingRefreshResult refresh = refreshService.refreshPendingBatches();
        log.info(
                "Driving dummy automation completed for all users. generatedBatches={}, attemptedUsers={}, processedBatches={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, failedFiles={}",
                generation.generatedBatches(),
                generation.attemptedUsers(),
                refresh.processedBatches(),
                refresh.insertedSessions(),
                refresh.insertedEvents(),
                refresh.updatedUsers(),
                refresh.failedFiles()
        );
        return new DummyDrivingAutomationResult(generation, refresh);
    }

    public DummyDrivingAutomationResult generateAndRefreshForUser(Long userId) {
        log.info("Driving dummy automation started for a single user. userId={}, pendingDir={}", userId, fileManager.getPendingDir());
        DummyDrivingGenerationResult generation = generationService.generateTodayBatchesForUser(userId, fileManager.getPendingDir());
        DummyDrivingRefreshResult refresh = refreshService.refreshPendingBatches();
        log.info(
                "Driving dummy automation completed for a single user. userId={}, generatedBatches={}, attemptedUsers={}, processedBatches={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, failedFiles={}",
                userId,
                generation.generatedBatches(),
                generation.attemptedUsers(),
                refresh.processedBatches(),
                refresh.insertedSessions(),
                refresh.insertedEvents(),
                refresh.updatedUsers(),
                refresh.failedFiles()
        );
        return new DummyDrivingAutomationResult(generation, refresh);
    }

    public DummyDrivingAutomationResult generateAndRefreshForUserVehicle(Long userId, Long userVehicleId) {
        log.info("Driving dummy automation started for a selected vehicle. userId={}, userVehicleId={}, pendingDir={}", userId, userVehicleId, fileManager.getPendingDir());
        DummyDrivingGenerationResult generation = generationService.generateTodayBatchesForUserVehicle(
                userId,
                userVehicleId,
                fileManager.getPendingDir()
        );
        List<Path> generatedFiles = generation.generatedFiles()
                .stream()
                .map(Path::of)
                .toList();
        DummyDrivingRefreshResult refresh = refreshService.refreshPendingBatches(generatedFiles);
        log.info(
                "Driving dummy automation completed for selected vehicle. userId={}, userVehicleId={}, generatedBatches={}, attemptedUsers={}, processedBatches={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, failedFiles={}",
                userId,
                userVehicleId,
                generation.generatedBatches(),
                generation.attemptedUsers(),
                refresh.processedBatches(),
                refresh.insertedSessions(),
                refresh.insertedEvents(),
                refresh.updatedUsers(),
                refresh.failedFiles()
        );
        return new DummyDrivingAutomationResult(generation, refresh);
    }
}
