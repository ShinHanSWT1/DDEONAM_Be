package com.gorani.ecodrive.driving.dto.query;

public record DrivingInsightResponse(
        String styleCode,
        String styleLabel,
        String summary,
        String insight,
        String tip,
        double confidence,
        String version,
        boolean fallbackUsed
) {
}
