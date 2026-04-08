package com.gorani.ecodrive.driving.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DrivingWeeklySummaryResponse(
        Integer year,
        Integer month,
        Integer weekOfMonth,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        Integer dayCount,
        Integer sessionCount,
        BigDecimal averageDistanceKm,
        BigDecimal averageIdlingTimeMinutes,
        BigDecimal averageSpeed,
        BigDecimal maxSpeed
) {
}
