package com.gorani.ecodrive.insurance.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import
        com.gorani.ecodrive.insurance.service.InsuranceCoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance")
public class InsuranceCoverageController {

    private final InsuranceCoverageService insuranceCoverageService;

    @GetMapping("/products/{productId}/coverages")
    public ApiResponse<?> getCoverages(
            @PathVariable Long productId,
            @RequestParam(required = false) String planType,
            @RequestParam(required = false) String category
    ) {
        List<InsuranceCoverage> coverages =
                insuranceCoverageService.getCoverages(productId, planType, category);
        List<CoverageResponse> result = coverages.stream()
                .map(c -> new CoverageResponse(
                        c.getId(),
                        c.getCategory(),
                        c.getCoverageName(),
                        c.getCoverageAmount(),
                        c.getIsRequired(),
                        c.getPlanType() != null ? c.getPlanType().name() : null,
                        c.getStatus() != null ? c.getStatus().name() : null
                ))
                .toList();
        return ApiResponse.success(new CoverageListResponse(result));
    }
    public record CoverageResponse(Long id, String category, String
    coverageName, Integer coverageAmount, Boolean isRequired, String
                                   planType, String status) {}
    public record CoverageListResponse(List<CoverageResponse>
                                       coverages) {}
}