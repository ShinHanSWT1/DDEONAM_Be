package com.gorani.ecodrive.driving.dto;

import java.time.LocalDate;

public record DrivingScoreHistoryResponse(
        Long id,
        String changeType,
        String message,
        Integer scoreDelta,
        LocalDate changeDate
) {
}
