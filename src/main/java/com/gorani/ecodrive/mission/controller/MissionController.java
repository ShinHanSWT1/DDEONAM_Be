package com.gorani.ecodrive.mission.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.mission.dto.MissionView;
import com.gorani.ecodrive.mission.service.MissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 미션 조회 API 제공 컨트롤러
 */
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    // 미션 조회/할당 보정 서비스
    private final MissionService missionService;

    /**
     * 일일 미션 목록 조회 메서드
     */
    @GetMapping("/daily")
    public ApiResponse<List<MissionView>> getDailyMissions(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success("성공적으로 처리되었습니다.", missionService.getDailyMissions(principal.getUserId()));
    }

    /**
     * 주간 미션 목록 조회 메서드
     */
    @GetMapping("/weekly")
    public ApiResponse<List<MissionView>> getWeeklyMissions(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ApiResponse.success("성공적으로 처리되었습니다.", missionService.getWeeklyMissions(principal.getUserId()));
    }
}
