package com.gorani.ecodrive.driving.dto.query;

import java.time.LocalDate;

public record DrivingBehaviorSummaryResponse(
        LocalDate sessionDate,
        Integer rapidAccelCount,
        Integer hardBrakeCount,
        Integer overspeedCount,
        Integer nightDrivingCount,
        Integer totalIdlingTimeMinutes
) {
}
