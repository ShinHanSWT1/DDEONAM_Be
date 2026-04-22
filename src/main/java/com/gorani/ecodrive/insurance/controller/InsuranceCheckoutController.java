package com.gorani.ecodrive.insurance.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutConfirmRequest;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutConfirmResponse;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutPrepareRequest;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutPrepareResponse;
import com.gorani.ecodrive.insurance.service.InsuranceCheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance/checkout")
public class InsuranceCheckoutController {

    private final InsuranceCheckoutService insuranceCheckoutService;

    @PostMapping("/prepare")
    public ApiResponse<InsuranceCheckoutPrepareResponse> prepareCheckout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody InsuranceCheckoutPrepareRequest request
    ) {
        log.info("보험 checkout 준비 API 요청. userId={}, insuranceProductId={}, userVehicleId={}, pointAmount={}",
                principal.getUserId(), request.insuranceProductId(), request.userVehicleId(), request.pointAmount());
        return ApiResponse.success(
                "보험 checkout 준비 성공",
                insuranceCheckoutService.prepareCheckout(principal.getUserId(), request)
        );
    }

    @PostMapping("/confirm")
    public ApiResponse<InsuranceCheckoutConfirmResponse> confirmCheckout(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody InsuranceCheckoutConfirmRequest request
    ) {
        log.info("보험 checkout 확정 API 요청. userId={}, orderId={}, paymentId={}, amount={}, status={}",
                principal.getUserId(), request.orderId(), request.paymentId(), request.amount(), request.status());
        return ApiResponse.success(
                "보험 checkout 확정 성공",
                insuranceCheckoutService.confirmCheckout(principal.getUserId(), request)
        );
    }
}
