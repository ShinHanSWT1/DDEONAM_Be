package com.gorani.ecodrive.driving.service.insight;

public record DrivingInsightClassification(
        DrivingInsightStyle style,
        int safetyScore,
        int efficiencyScore,
        int stabilityScore
) {
}
