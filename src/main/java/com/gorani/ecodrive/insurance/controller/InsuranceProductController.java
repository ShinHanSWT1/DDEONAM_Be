package com.gorani.ecodrive.insurance.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import com.gorani.ecodrive.insurance.service.InsuranceProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance")
public class InsuranceProductController {

    private final InsuranceProductService insuranceProductService;

    @GetMapping("/products")
    public ApiResponse<?> getProducts(
            @RequestParam(required = false) Long insuranceCompanyId,
            @RequestParam(required = false) String status
    ) {
        List<InsuranceProduct> products = insuranceProductService.getProducts(insuranceCompanyId, status);
        List<ProductResponse> result = products.stream()
                .map(p -> new ProductResponse(
                        p.getId(),
                        p.getInsuranceCompany().getId(),
                        p.getInsuranceCompany().getCompanyName(),
                        p.getProductName(),
                        p.getBaseAmount(),
                        p.getStatus().name()
                ))
                .toList();
        return ApiResponse.success(new ProductListResponse(result));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<?> getProduct(@PathVariable Long productId) {
        InsuranceProduct product = insuranceProductService.getProductById(productId);
        return ApiResponse.success(new ProductDetailResponse(
                product.getId(),
                product.getInsuranceCompany().getId(),
                product.getInsuranceCompany().getCompanyName(),
                product.getProductName(),
                product.getBaseAmount(),
                product.getStatus().name(),
                product.getCreatedAt()
        ));
    }

    public record ProductResponse(Long id, Long insuranceCompanyId, String companyName, String
    productName, Integer baseAmount, String status) {}
    public record ProductListResponse(List<ProductResponse> products) {}
    public record ProductDetailResponse(Long id, Long insuranceCompanyId, String companyName, String
    productName, Integer baseAmount, String status, LocalDateTime createdAt) {}
}