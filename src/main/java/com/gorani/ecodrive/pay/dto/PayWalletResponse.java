package com.gorani.ecodrive.pay.dto;

// 계좌 응답 DTO
public record PayWalletResponse(
        // Pay 사용자 식별자
        Long payUserId,
        // Pay 계좌 식별자
        Long payAccountId,
        // 계좌번호
        String accountNumber,
        // 은행 코드
        String bankCode,
        // 예금주명
        String ownerName,
        // 잔액
        Integer balance,
        // 포인트 잔액
        Long points,
        // 계좌 상태
        String status
) {
}
