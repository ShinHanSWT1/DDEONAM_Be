package com.gorani.ecodrive.driving.dto.query;

import java.time.LocalDate;

public record DrivingScoreHistoryResponse(
        Long id,
        String changeType,
        String message,
        Integer scoreDelta,
        LocalDate changeDate
) {
}
