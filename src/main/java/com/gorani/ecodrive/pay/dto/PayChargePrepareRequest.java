package com.gorani.ecodrive.pay.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayChargePrepareRequest(
        @NotNull
        @Min(1000)
        Integer amount
) {
}
