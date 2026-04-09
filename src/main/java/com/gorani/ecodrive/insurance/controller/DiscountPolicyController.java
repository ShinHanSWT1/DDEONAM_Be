package com.gorani.ecodrive.insurance.controller;

import com.gorani.ecodrive.common.response.ApiResponse;
import com.gorani.ecodrive.insurance.service.DiscountCalculationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/insurance/discount-policies")
@Validated
public class DiscountPolicyController {

    private final DiscountCalculationService discountCalculationService;

    @GetMapping("/calculate")
    public ApiResponse<?> calculate(
            @RequestParam @Min(0) @Max(150) int age,
            @RequestParam @Min(0) @Max(100) int score,
            @RequestParam @Min(0) int experienceYears
    ) {
        double ageFactor = discountCalculationService.calculateAgeFactor(age);
        double experienceFactor = discountCalculationService.calculateExperienceFactor(experienceYears);
        double scoreDiscountRate = discountCalculationService.calculateScoreDiscountRate(age, score);

        return ApiResponse.success(new CalculateResponse(
                ageFactor,
                experienceFactor,
                BigDecimal.valueOf(scoreDiscountRate)
        ));
    }

    public record CalculateResponse(
            double ageFactor,
            double experienceFactor,
            BigDecimal scoreDiscountRate
    ) {}
}
