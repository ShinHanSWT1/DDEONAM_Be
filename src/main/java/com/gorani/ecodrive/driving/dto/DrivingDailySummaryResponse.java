package com.gorani.ecodrive.driving.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DrivingDailySummaryResponse(
        LocalDate date,
        Integer sessionCount,
        BigDecimal totalDistanceKm,
        Integer totalDrivingTimeMinutes,
        Integer totalIdlingTimeMinutes,
        BigDecimal averageSpeed,
        BigDecimal maxSpeed,
        Integer rapidAccelCount,
        Integer hardBrakeCount,
        Integer overspeedCount,
        LocalDateTime firstStartedAt,
        LocalDateTime lastEndedAt
) {
}
