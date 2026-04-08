package com.gorani.ecodrive.driving.dto;

import java.time.LocalDate;

public record DrivingScoreTrendResponse(
        LocalDate snapshotDate,
        Integer score
) {
}
