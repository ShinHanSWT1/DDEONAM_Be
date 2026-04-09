package com.gorani.ecodrive.driving.dto.query;

import java.time.LocalDate;

public record DrivingScoreTrendResponse(
        LocalDate snapshotDate,
        Integer score
) {
}
