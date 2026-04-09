package com.gorani.ecodrive.insurance.domain;

public enum InsuranceContractStatus {
    PENDING,    // 결제 대기
    ACTIVE,     // 계약 활성
    EXPIRED,    // 만료
    CANCELLED   // 해지
}