package com.gorani.ecodrive.insurance.service;

import org.springframework.stereotype.Service;

@Service
public class DiscountCalculationService {

    // 보험료
    public int calculateBasePremium(int age) {
        if (age >= 20 && age <= 25) return 1_100_000;
        if (age >= 26 && age <= 30) return 900_000;
        if (age >= 31 && age <= 40) return 600_000;
        return 400_000; // 41세 이상
    }

    // 운전 경력
    public double calculateExperienceFactor(int experienceYears) {
        if (experienceYears < 1) return 1.30;   // 1년 미만 30% 할증
        if (experienceYears < 3) return 1.105;  // 1~3년 할증에서 15% 할인
        return 1.00;                             // 3년 이상 기본
    }

    // 안전운전 점수 + 나이 조합 할인율
    public double calculateScoreDiscountRate(int age, int score) {
        if (score >= 95) return age <= 28 ? 0.265 : 0.175;
        if (score >= 91) return age <= 28 ? 0.211 : 0.115;
        if (score >= 81) return age <= 28 ? 0.149 : 0.045;
        return 0.0; // 81점 미만 할인 없음
    }

    // 최종 보험료 계산
    public int calculateFinalPremium(int age, int score, int experienceYears) {
        int basePremium = calculateBasePremium(age);
        double afterExperience = basePremium * calculateExperienceFactor(experienceYears);
        double discountRate = calculateScoreDiscountRate(age, score);
        return (int) (afterExperience * (1 - discountRate));
    }
}