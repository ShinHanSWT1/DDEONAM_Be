package com.gorani.ecodrive.driving.dto;

import java.time.LocalDate;

public record DrivingLatestScoreResponse(
        LocalDate snapshotDate,
        Integer score
) {
}
