package com.gorani.ecodrive.insurance.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.insurance.domain.InsuranceCompany;
import com.gorani.ecodrive.insurance.service.InsuranceCompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance")
public class InsuranceCompanyController {

    private final InsuranceCompanyService insuranceCompanyService;

    @GetMapping("/companies")
    public ApiResponse<?> getCompanies(
            @RequestParam(required = false) String status
    ) {
        List<InsuranceCompany> companies = insuranceCompanyService.getCompanies(status);
        List<CompanyResponse> result = companies.stream()
                .map(c -> new CompanyResponse(c.getId(), c.getCompanyName(), c.getCode(), c.getStatus().name()))
                .toList();
        return ApiResponse.success(new CompanyListResponse(result));
    }

    @GetMapping("/companies/{companyId}")
    public ApiResponse<?> getCompany(@PathVariable Long companyId) {
        InsuranceCompany company = insuranceCompanyService.getCompanyById(companyId);
        return ApiResponse.success(new CompanyDetailResponse(
                company.getId(), company.getCompanyName(), company.getCode(),
                company.getStatus().name(), company.getCreatedAt()
        ));
    }

    public record CompanyResponse(Long id, String companyName, String code, String status) {}
    public record CompanyListResponse(List<CompanyResponse> companies) {}
    public record CompanyDetailResponse(Long id, String companyName, String code, String status, LocalDateTime createdAt) {}
}
