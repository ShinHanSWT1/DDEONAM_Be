package com.gorani.ecodrive.pay.dto;

// 결제 응답 DTO
public record PayCheckoutResponse(
        // 결제 식별자
        Long paymentId,
        // 외부 주문 식별자
        String externalOrderId,
        // 결제 상태
        String status,
        // 결제 금액
        Integer amount,
        // 결제 타입
        String paymentType,
        // Pay 사용자 식별자
        Long payUserId,
        // Pay 계좌 식별자
        Long payAccountId,
        // 결제 후 잔액
        Integer balanceAfterPayment
) {
}
