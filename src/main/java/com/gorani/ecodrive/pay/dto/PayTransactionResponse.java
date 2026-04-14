package com.gorani.ecodrive.pay.dto;

public record PayTransactionResponse(
        Long id,
        String title,
        String date,
        Integer amount,
        String type,
        String category
) {
}
