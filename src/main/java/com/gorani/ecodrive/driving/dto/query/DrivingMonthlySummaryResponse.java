package com.gorani.ecodrive.driving.dto.query;

import java.math.BigDecimal;

public record DrivingMonthlySummaryResponse(
        Integer year,
        Integer month,
        Integer sessionCount,
        Integer dayCount,
        BigDecimal totalDistanceKm,
        Integer totalDrivingTimeMinutes,
        Integer totalIdlingTimeMinutes,
        BigDecimal averageSpeed,
        BigDecimal maxSpeed,
        Integer rapidAccelCount,
        Integer hardBrakeCount,
        Integer overspeedCount,
        BigDecimal steadyDrivingRatio,
        BigDecimal carbonReductionKg,
        Integer rewardPoint
) {
}
