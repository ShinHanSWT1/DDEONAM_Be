package com.gorani.ecodrive.driving.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingAutomationResult;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingRefreshResult;
import com.gorani.ecodrive.driving.service.ingestion.DrivingDummyAutomationService;
import com.gorani.ecodrive.driving.service.ingestion.DrivingDummyRefreshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driving/admin")
public class DrivingDummyRefreshController {

    private final DrivingDummyAutomationService drivingDummyAutomationService;
    private final DrivingDummyRefreshService drivingDummyRefreshService;

    @PostMapping("/generate-and-refresh-dummy-data")
    public ApiResponse<DummyDrivingAutomationResult> generateAndRefreshDummyDrivingData(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("Driving dummy generate+refresh requested. userId={}", principal.getUserId());
        DummyDrivingAutomationResult result =
                drivingDummyAutomationService.generateAndRefreshForUser(principal.getUserId());
        log.info(
                "Driving dummy generate+refresh completed. userId={}, generatedBatches={}, attemptedUsers={}, processedBatches={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, failedFiles={}",
                principal.getUserId(),
                result.generation().generatedBatches(),
                result.generation().attemptedUsers(),
                result.refresh().processedBatches(),
                result.refresh().insertedSessions(),
                result.refresh().insertedEvents(),
                result.refresh().updatedUsers(),
                result.refresh().failedFiles()
        );

        return ApiResponse.success(
                "Driving dummy data generation and refresh completed",
                result
        );
    }

    @PostMapping("/refresh-dummy-data")
    public ApiResponse<DummyDrivingRefreshResult> refreshDummyDrivingData() {
        log.info("Driving dummy refresh requested.");
        DummyDrivingRefreshResult result = drivingDummyRefreshService.refreshPendingBatches();
        log.info(
                "Driving dummy refresh completed. processedBatches={}, insertedSessions={}, insertedEvents={}, updatedUsers={}, failedFiles={}",
                result.processedBatches(),
                result.insertedSessions(),
                result.insertedEvents(),
                result.updatedUsers(),
                result.failedFiles()
        );

        return ApiResponse.success(
                "Driving dummy data refresh completed",
                result
        );
    }
}