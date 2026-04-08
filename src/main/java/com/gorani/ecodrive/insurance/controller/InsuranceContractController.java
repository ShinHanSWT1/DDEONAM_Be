package com.gorani.ecodrive.insurance.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.common.security.CustomUserPrincipal;
import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.service.InsuranceContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance")
public class InsuranceContractController {

    private final InsuranceContractService insuranceContractService;

    @GetMapping("/contracts/{contractId}")
    public ApiResponse<?> getContract(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        InsuranceContract contract = insuranceContractService.getContract(contractId, principal.getUserId());
        return ApiResponse.success(new ContractDetailResponse(
                contract.getId(),
                contract.getUser().getId(),
                contract.getInsuranceProduct().getId(),
                contract.getInsuranceProduct().getProductName(),
                contract.getInsuranceProduct().getInsuranceCompany().getCompanyName(),
                contract.getPhoneNumber(),
                contract.getAddress(),
                contract.getContractPeriod(),
                contract.getPlanType() != null ? contract.getPlanType().name() : null,
                contract.getStatus() != null ? contract.getStatus().name() : null,
                contract.getStartedAt(),
                contract.getEndedAt(),
                contract.getBaseAmount(),
                contract.getDiscountRate(),
                contract.getDiscountAmount(),
                contract.getFinalAmount(),
                contract.getCreatedAt()
        ));
    }

    @GetMapping("/contracts")
    public ApiResponse<?> getMyContracts(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        List<InsuranceContract> contracts = insuranceContractService.getMyContracts(principal.getUserId(), status);
        List<ContractResponse> result = contracts.stream()
                .map(c -> new ContractResponse(
                        c.getId(),
                        c.getInsuranceProduct().getProductName(),
                        c.getInsuranceProduct().getInsuranceCompany().getCompanyName(),
                        c.getPlanType() != null ? c.getPlanType().name() : null,
                        c.getStatus() != null ? c.getStatus().name() : null,
                        c.getFinalAmount(),
                        c.getStartedAt(),
                        c.getEndedAt()
                ))
                .toList();
        return ApiResponse.success(new ContractListResponse(result));
    }

    @PostMapping("/contracts")
    public ApiResponse<?> createContract(
            @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        InsuranceContract contract = insuranceContractService.createContract(principal.getUserId(), request);
        return ApiResponse.success(new ContractDetailResponse(
                contract.getId(),
                contract.getUser().getId(),
                contract.getInsuranceProduct().getId(),
                contract.getInsuranceProduct().getProductName(),
                contract.getInsuranceProduct().getInsuranceCompany().getCompanyName(),
                contract.getPhoneNumber(),
                contract.getAddress(),
                contract.getContractPeriod(),
                contract.getPlanType() != null ? contract.getPlanType().name() : null,
                contract.getStatus() != null ? contract.getStatus().name() : null,
                contract.getStartedAt(),
                contract.getEndedAt(),
                contract.getBaseAmount(),
                contract.getDiscountRate(),
                contract.getDiscountAmount(),
                contract.getFinalAmount(),
                contract.getCreatedAt()
        ));
    }

    @PatchMapping("/contracts/{contractId}/cancel")
    public ApiResponse<?> cancelContract(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        insuranceContractService.cancelContract(contractId, principal.getUserId());
        return ApiResponse.success(new CancelResponse(contractId, "CANCELLED"));
    }

    public record ContractDetailResponse(Long id, Long userId, Long insuranceProductId, String productName,
                                         String companyName, String phoneNumber, String address,
                                         Integer contractPeriod, String planType, String status,
                                         LocalDateTime startedAt, LocalDateTime endedAt,
                                         Integer baseAmount, BigDecimal discountRate,
                                         Integer discountAmount, Integer finalAmount,
                                         LocalDateTime createdAt) {}
    public record ContractResponse(Long id, String productName, String companyName,
                                   String planType, String status, Integer finalAmount,
                                   LocalDateTime startedAt, LocalDateTime endedAt) {}
    public record ContractListResponse(List<ContractResponse> contracts) {}
    public record CancelResponse(Long id, String status) {}
    public record CreateContractRequest(
            Long insuranceProductId,
            String phoneNumber,
            String address,
            Integer contractPeriod,
            String planType,
            List<Long> selectedCoverageIds
    ) {}

}
