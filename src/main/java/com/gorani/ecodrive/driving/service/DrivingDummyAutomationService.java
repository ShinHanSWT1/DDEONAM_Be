package com.gorani.ecodrive.driving.service;

import com.gorani.ecodrive.driving.dto.DummyDrivingAutomationResult;
import com.gorani.ecodrive.driving.dto.DummyDrivingGenerationResult;
import com.gorani.ecodrive.driving.dto.DummyDrivingRefreshResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrivingDummyAutomationService {

    private final DrivingDummyGenerationService generationService;
    private final DrivingDummyRefreshService refreshService;
    private final DrivingDummyFileManager fileManager;

    public DummyDrivingAutomationResult generateAndRefresh() {
        DummyDrivingGenerationResult generation = generationService.generateTodayBatches(fileManager.getPendingDir());
        DummyDrivingRefreshResult refresh = refreshService.refreshPendingBatches();
        return new DummyDrivingAutomationResult(generation, refresh);
    }
}
