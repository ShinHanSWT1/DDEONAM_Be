package com.gorani.ecodrive.driving.dto.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DummyDrivingSessionPayload(
        String externalKey,
        Long userId,
        Long userVehicleId,
        LocalDate sessionDate,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        BigDecimal distanceKm,
        Integer drivingTimeMinutes,
        Integer idlingTimeMinutes,
        BigDecimal averageSpeed,
        BigDecimal maxSpeed,
        String driverStyle,
        String vehicleSize,
        String fuelType,
        Integer rapidAccelCount,
        Integer hardBrakeCount,
        Integer overspeedCount,
        Integer safetyScore,
        Integer ecoScore,
        BigDecimal carbonEmissionKg,
        BigDecimal carbonReductionKg,
        Integer rewardPoint,
        List<DummyDrivingEventPayload> events
) {
}
