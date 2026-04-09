package com.gorani.ecodrive.driving.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingAutomationResult;
import com.gorani.ecodrive.driving.dto.ingestion.DummyDrivingRefreshResult;
import com.gorani.ecodrive.driving.service.ingestion.DrivingDummyAutomationService;
import com.gorani.ecodrive.driving.service.ingestion.DrivingDummyRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driving/admin")
public class DrivingDummyRefreshController {

    private final DrivingDummyAutomationService drivingDummyAutomationService;
    private final DrivingDummyRefreshService drivingDummyRefreshService;

    @PostMapping("/generate-and-refresh-dummy-data")
    public ApiResponse<DummyDrivingAutomationResult> generateAndRefreshDummyDrivingData() {
        return ApiResponse.success(
                "주행 더미데이터 생성 및 반영이 완료되었습니다.",
                drivingDummyAutomationService.generateAndRefresh()
        );
    }

    @PostMapping("/refresh-dummy-data")
    public ApiResponse<DummyDrivingRefreshResult> refreshDummyDrivingData() {
        return ApiResponse.success(
                "주행 더미데이터 반영이 완료되었습니다.",
                drivingDummyRefreshService.refreshPendingBatches()
        );
    }
}
