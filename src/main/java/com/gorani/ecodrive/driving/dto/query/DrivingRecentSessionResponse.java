package com.gorani.ecodrive.driving.dto.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DrivingRecentSessionResponse(
        Long id,
        String externalKey,
        LocalDate sessionDate,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        BigDecimal distanceKm,
        Integer drivingTimeMinutes,
        Integer idlingTimeMinutes,
        BigDecimal averageSpeed,
        BigDecimal maxSpeed
) {
}
