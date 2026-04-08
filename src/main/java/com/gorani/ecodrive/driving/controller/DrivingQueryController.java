package com.gorani.ecodrive.driving.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.driving.dto.DrivingLatestCarbonResponse;
import com.gorani.ecodrive.driving.dto.DrivingLatestScoreResponse;
import com.gorani.ecodrive.driving.dto.DrivingRecentSessionResponse;
import com.gorani.ecodrive.driving.service.DrivingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driving")
public class DrivingQueryController {

    private final DrivingQueryService drivingQueryService;

    @GetMapping("/scores/latest")
    public ApiResponse<DrivingLatestScoreResponse> getLatestScore(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success(
                "최신 운전점수 조회 성공",
                drivingQueryService.getLatestScore(principal.getUserId())
        );
    }

    @GetMapping("/sessions/recent")
    public ApiResponse<List<DrivingRecentSessionResponse>> getRecentSessions(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.success(
                "최근 주행 세션 조회 성공",
                drivingQueryService.getRecentSessions(principal.getUserId(), Math.max(1, Math.min(limit, 20)))
        );
    }

    @GetMapping("/carbon/latest")
    public ApiResponse<DrivingLatestCarbonResponse> getLatestCarbon(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success(
                "최신 탄소 절감량 조회 성공",
                drivingQueryService.getLatestCarbon(principal.getUserId())
        );
    }
}
