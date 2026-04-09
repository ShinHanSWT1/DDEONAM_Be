package com.gorani.ecodrive.driving.service.ingestion;

import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingAutomationResult;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingGenerationResult;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingRefreshResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrivingDummyAutomationService {

    private final DrivingDummyGenerationService generationService;
    private final DrivingDummyRefreshService refreshService;
    private final DrivingDummyFileManager fileManager;

    public DummyDrivingAutomationResult generateAndRefreshForAllUsers() {
        DummyDrivingGenerationResult generation = generationService.generateTodayBatches(fileManager.getPendingDir());
        DummyDrivingRefreshResult refresh = refreshService.refreshPendingBatches();
        return new DummyDrivingAutomationResult(generation, refresh);
    }

    public DummyDrivingAutomationResult generateAndRefreshForUser(Long userId) {
        DummyDrivingGenerationResult generation = generationService.generateTodayBatchesForUser(userId, fileManager.getPendingDir());
        DummyDrivingRefreshResult refresh = refreshService.refreshPendingBatches();
        return new DummyDrivingAutomationResult(generation, refresh);
    }
}
