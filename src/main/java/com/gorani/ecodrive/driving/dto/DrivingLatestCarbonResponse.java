package com.gorani.ecodrive.driving.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DrivingLatestCarbonResponse(
        LocalDate snapshotDate,
        BigDecimal carbonReductionKg,
        Integer rewardPoint
) {
}
