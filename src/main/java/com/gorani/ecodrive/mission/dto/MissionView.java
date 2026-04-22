package com.gorani.ecodrive.mission.dto;

import com.gorani.ecodrive.mission.domain.MissionCategory;
import com.gorani.ecodrive.mission.domain.MissionStatus;
import com.gorani.ecodrive.mission.domain.MissionTargetType;
import com.gorani.ecodrive.mission.domain.MissionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 미션 조회 응답 DTO
 */
public record MissionView(
        Long userMissionId,
        Long missionPolicyId,
        String title,
        String description,
        MissionType missionType,
        MissionCategory category,
        MissionTargetType targetType,
        BigDecimal targetValue,
        BigDecimal currentValue,
        BigDecimal progressRate,
        Integer rewardPoint,
        MissionStatus status,
        Short slotNo,
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        LocalDateTime rewardedAt
) {
}
