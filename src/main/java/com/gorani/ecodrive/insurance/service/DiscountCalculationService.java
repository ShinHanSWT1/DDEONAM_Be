package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class DiscountCalculationService {

    // 나이별 보정계수 (base_amount에 곱함)
    public double calculateAgeFactor(int age) {
        if (age < 20) throw new CustomException(ErrorCode.INVALID_AGE_FOR_INSURANCE);
        if (age >= 20 && age <= 25) return 1.30;   // 20~25세 30% 할증
        if (age >= 26 && age <= 30) return 1.10;   // 26~30세 10% 할증
        if (age >= 31 && age <= 40) return 1.00;   // 31~40세 기본
        return 0.90;                                 // 41세 이상 10% 할인
    }

    // 운전 경력 보정계수
    public double calculateExperienceFactor(int experienceYears) {
        if (experienceYears < 1) return 1.30;   // 1년 미만 30% 할증
        if (experienceYears < 3) return 1.105;  // 1~3년 10.5% 할증
        return 1.00;                             // 3년 이상 기본
    }

    // 안전운전 점수 + 나이 + 주행거리 조합 할인율
    public double calculateScoreDiscountRate(int age, int score, int annualMileageKm) {
        if (annualMileageKm == 0 || annualMileageKm > 15000) return 0.0;
        if (score >= 95) return age <= 28 ? 0.265 : 0.175;
        if (score >= 91) return age <= 28 ? 0.211 : 0.115;
        if (score >= 80) return age <= 28 ? 0.149 : 0.045;
        return 0.0; // 80점 미만 할인 없음
    }

    // 플랜별 배율
    public double calculatePlanFactor(com.gorani.ecodrive.insurance.domain.InsurancePlanType planType) {
        return switch (planType) {
            case BASIC -> 0.8;
            case STANDARD -> 1.0;
            case PREMIUM -> 1.3;
        };
    }

    // 최종 보험료 계산 (상품 base_amount 기준)
    public int calculateFinalPremium(int baseAmount, int age, int score, int experienceYears, int annualMileageKm) {
        double afterAge = baseAmount * calculateAgeFactor(age);
        double afterExperience = afterAge * calculateExperienceFactor(experienceYears);
        double discountRate = calculateScoreDiscountRate(age, score, annualMileageKm);
        return (int) (afterExperience * (1 - discountRate));
    }
}
