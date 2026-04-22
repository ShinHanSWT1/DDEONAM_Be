package com.gorani.ecodrive.insurance.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.insurance.domain.UserInsurance;
import com.gorani.ecodrive.insurance.service.UserInsuranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/insurances")
public class UserInsuranceController {

    private final UserInsuranceService userInsuranceService;

    @GetMapping
    public ApiResponse<?> getMyInsurances(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<UserInsurance> insurances = userInsuranceService.getMyInsurances(principal.getUserId());
        List<InsuranceResponse> result = insurances.stream()
                .map(i -> new InsuranceResponse(
                        i.getId(),
                        i.getUserVehicleId(),
                        i.getInsuranceCompany().getId(),
                        i.getInsuranceCompany().getCompanyName(),
                        i.getInsuranceProduct().getId(),
                        i.getInsuranceProduct().getProductName(),
                        i.getInsuranceContract().getId(),
                        i.getInsuranceContract().getPlanType() != null ? i.getInsuranceContract().getPlanType().name() : null,
                        i.getInsuranceContract().getBaseAmount(),
                        i.getInsuranceContract().getFinalAmount(),
                        i.getStatus().name(),
                        i.getCreatedAt()
                ))
                .toList();
        return ApiResponse.success(new InsuranceListResponse(result));
    }

    @GetMapping("/{insuranceId}")
    public ApiResponse<?> getMyInsurance(
            @PathVariable Long insuranceId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserInsurance insurance = userInsuranceService.getMyInsurance(insuranceId, principal.getUserId());
        return ApiResponse.success(new InsuranceDetailResponse(
                insurance.getId(),
                insurance.getUserVehicleId(),
                insurance.getInsuranceCompany().getId(),
                insurance.getInsuranceCompany().getCompanyName(),
                insurance.getInsuranceProduct().getId(),
                insurance.getInsuranceProduct().getProductName(),
                insurance.getInsuranceContract().getId(),
                insurance.getInsuranceContract().getPlanType() != null ? insurance.getInsuranceContract().getPlanType().name() : null,
                insurance.getInsuranceContract().getBaseAmount(),
                insurance.getInsuranceContract().getFinalAmount(),
                insurance.getInsuranceContract().getStartedAt(),
                insurance.getInsuranceContract().getEndedAt(),
                insurance.getStatus().name(),
                insurance.getCreatedAt()
        ));
    }

    @PostMapping
    public ApiResponse<?> confirmInsurance(
            @RequestBody ConfirmInsuranceRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        UserInsurance insurance = userInsuranceService.confirmInsurance(
                principal.getUserId(), request.insuranceContractsId(), request.userVehicleId());
        return ApiResponse.success(new ConfirmInsuranceResponse(
                insurance.getId(),
                insurance.getInsuranceContract().getStatus().name(),
                insurance.getInsuranceContract().getFinalAmount(),
                insurance.getCreatedAt()
        ));
    }

    public record InsuranceResponse(Long id, Long userVehicleId, Long insuranceCompanyId, String companyName,
                                    Long insuranceProductId, String productName,
                                    Long insuranceContractsId, String planType, 
                                    Integer baseAmount, Integer finalAmount, String status,
                                    LocalDateTime createdAt) {}
    public record InsuranceListResponse(List<InsuranceResponse> insurances) {}
    public record InsuranceDetailResponse(Long id, Long userVehicleId, Long insuranceCompanyId, String companyName,
                                          Long insuranceProductId, String productName,
                                          Long insuranceContractsId, String planType,
                                          Integer baseAmount, Integer finalAmount, 
                                          LocalDateTime startedAt, LocalDateTime endedAt, 
                                          String status, LocalDateTime createdAt) {}
    public record ConfirmInsuranceRequest(Long insuranceContractsId, Long userVehicleId) {}
    public record ConfirmInsuranceResponse(Long id, String status, Integer finalAmount, LocalDateTime createdAt) {}
}
