package com.gorani.ecodrive.pay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 충전 요청 DTO
public record PayChargeRequest(
        // 충전 금액
        @NotNull
        @Min(1)
        Integer amount
) {
}
