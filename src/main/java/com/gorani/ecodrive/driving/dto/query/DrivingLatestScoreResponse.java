package com.gorani.ecodrive.driving.dto.query;

import java.time.LocalDate;

public record DrivingLatestScoreResponse(
        LocalDate snapshotDate,
        Integer score
) {
}
