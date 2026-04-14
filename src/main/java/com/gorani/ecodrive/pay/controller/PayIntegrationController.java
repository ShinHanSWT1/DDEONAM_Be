package com.gorani.ecodrive.pay.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.pay.dto.PayChargeRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutResponse;
import com.gorani.ecodrive.pay.dto.PayTransactionResponse;
import com.gorani.ecodrive.pay.dto.PayWalletResponse;
import com.gorani.ecodrive.pay.service.PayIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayIntegrationController {

    private final PayIntegrationService payIntegrationService;

    @GetMapping("/account")
    public ApiResponse<PayWalletResponse> getMyWallet(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("Pay 지갑 조회 API 요청. userId={}", principal.getUserId());
        return ApiResponse.success(
                "Pay 지갑 조회 성공",
                payIntegrationService.getWallet(principal.getUserId())
        );
    }

    @PostMapping("/account")
    public ApiResponse<PayWalletResponse> createMyWallet(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("Pay 지갑 생성 API 요청. userId={}", principal.getUserId());
        return ApiResponse.success(
                "Pay 지갑 생성 성공",
                payIntegrationService.createWallet(principal.getUserId())
        );
    }

    @GetMapping("/transactions")
    public ApiResponse<List<PayTransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        log.info("Pay 거래내역 조회 API 요청. userId={}", principal.getUserId());
        return ApiResponse.success(
                "Pay 거래내역 조회 성공",
                payIntegrationService.getTransactions(principal.getUserId())
        );
    }

    @PostMapping("/charge")
    public ApiResponse<PayWalletResponse> charge(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody PayChargeRequest request
    ) {
        log.info("Pay 충전 API 요청. userId={}, amount={}", principal.getUserId(), request.amount());
        return ApiResponse.success(
                "Pay 충전 성공",
                payIntegrationService.charge(principal.getUserId(), request)
        );
    }

    @PostMapping("/checkout")
    public ApiResponse<PayCheckoutResponse> checkout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody PayCheckoutRequest request
    ) {
        log.info("Pay 결제 API 요청. userId={}, title={}, amount={}",
                principal.getUserId(), request.title(), request.amount());
        return ApiResponse.success(
                "Pay 결제 성공",
                payIntegrationService.checkout(principal.getUserId(), request)
        );
    }
}
