package com.gorani.ecodrive.driving.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.driving.dto.query.DrivingBehaviorSummaryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingDailySummaryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingLatestCarbonResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingLatestScoreResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingMonthlySummaryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingRecentSessionResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingScoreHistoryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingScoreTrendResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingWeeklySummaryResponse;
import com.gorani.ecodrive.driving.service.query.DrivingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driving")
public class DrivingQueryController {

    private static final int MAX_RECENT_SESSION_LIMIT = 180;
    private static final int MAX_SCORE_HISTORY_LIMIT = 20;

    private final DrivingQueryService drivingQueryService;

    @GetMapping("/scores/latest")
    public ApiResponse<DrivingLatestScoreResponse> getLatestScore(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId
    ) {
        return ApiResponse.success(
                "최신 운전점수 조회 성공",
                drivingQueryService.getLatestScore(principal.getUserId(), userVehicleId)
        );
    }

    @GetMapping("/sessions/recent")
    public ApiResponse<List<DrivingRecentSessionResponse>> getRecentSessions(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.success(
                "최근 주행 세션 조회 성공",
                drivingQueryService.getRecentSessions(
                        principal.getUserId(),
                        userVehicleId,
                        Math.max(1, Math.min(limit, MAX_RECENT_SESSION_LIMIT))
                )
        );
    }

    @GetMapping("/carbon/latest")
    public ApiResponse<DrivingLatestCarbonResponse> getLatestCarbon(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId
    ) {
        return ApiResponse.success(
                "최신 탄소 절감량 조회 성공",
                drivingQueryService.getLatestCarbon(principal.getUserId(), userVehicleId)
        );
    }

    @GetMapping("/daily-summary")
    public ApiResponse<DrivingDailySummaryResponse> getDailySummary(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam LocalDate date
    ) {
        return ApiResponse.success(
                "일자별 주행 요약 조회 성공",
                drivingQueryService.getDailySummary(principal.getUserId(), userVehicleId, date)
        );
    }

    @GetMapping("/daily-summaries")
    public ApiResponse<List<DrivingDailySummaryResponse>> getDailySummaries(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(
                "월간 일자별 주행 요약 조회 성공",
                drivingQueryService.getDailySummaries(principal.getUserId(), userVehicleId, year, month)
        );
    }

    @GetMapping("/behavior-summary")
    public ApiResponse<DrivingBehaviorSummaryResponse> getBehaviorSummary(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam LocalDate date
    ) {
        return ApiResponse.success(
                "일자별 주행 행동 요약 조회 성공",
                drivingQueryService.getBehaviorSummary(principal.getUserId(), userVehicleId, date)
        );
    }

    @GetMapping("/weekly-summaries")
    public ApiResponse<List<DrivingWeeklySummaryResponse>> getWeeklySummaries(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(
                "주차별 주행 요약 조회 성공",
                drivingQueryService.getWeeklySummaries(principal.getUserId(), userVehicleId, year, month)
        );
    }

    @GetMapping("/monthly-summary")
    public ApiResponse<DrivingMonthlySummaryResponse> getMonthlySummary(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(
                "월별 주행 요약 조회 성공",
                drivingQueryService.getMonthlySummary(principal.getUserId(), userVehicleId, year, month)
        );
    }

    @GetMapping("/scores/trend")
    public ApiResponse<List<DrivingScoreTrendResponse>> getScoreTrend(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ApiResponse.success(
                "점수 추이 조회 성공",
                drivingQueryService.getScoreTrend(principal.getUserId(), userVehicleId, year, month)
        );
    }

    @GetMapping("/scores/history")
    public ApiResponse<List<DrivingScoreHistoryResponse>> getScoreHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) Long userVehicleId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(
                "점수 변화 이력 조회 성공",
                drivingQueryService.getScoreHistory(
                        principal.getUserId(),
                        userVehicleId,
                        Math.max(1, Math.min(limit, MAX_SCORE_HISTORY_LIMIT))
                )
        );
    }
}
