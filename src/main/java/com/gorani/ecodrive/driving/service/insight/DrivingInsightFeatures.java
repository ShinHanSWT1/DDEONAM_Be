package com.gorani.ecodrive.driving.service.insight;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record DrivingInsightFeatures(
        int sessionCount,
        BigDecimal distanceKm,
        int drivingMinutes,
        double avgSpeed,
        double maxSpeed,
        double hardAccelPer100km,
        double hardBrakePer100km,
        double overspeedEventRatio,
        double idlingRatio,
        double nightDrivingRatio
) {
    public boolean isInsufficientForInsight() {
        return sessionCount < 3 && distanceKm.compareTo(BigDecimal.valueOf(30)) < 0;
    }

    public String fingerprint() {
        return String.join(":",
                String.valueOf(sessionCount),
                scaled(distanceKm.doubleValue()),
                String.valueOf(drivingMinutes),
                scaled(avgSpeed),
                scaled(maxSpeed),
                scaled(hardAccelPer100km),
                scaled(hardBrakePer100km),
                scaled(overspeedEventRatio),
                scaled(idlingRatio),
                scaled(nightDrivingRatio)
        );
    }

    public double overspeedEventPercentage() {
        return overspeedEventRatio * 100.0;
    }

    private static String scaled(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP)
                .toPlainString();
    }
}
