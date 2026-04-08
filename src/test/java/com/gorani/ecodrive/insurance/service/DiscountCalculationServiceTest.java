package com.gorani.ecodrive.insurance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DiscountCalculationServiceTest {

    private DiscountCalculationService service;

    @BeforeEach
    void setUp() {
        service = new DiscountCalculationService();
    }

    @Nested
    @DisplayName("calculateBasePremium - 나이별 기본 보험료")
    class CalculateBasePremiumTest {

        @Test
        @DisplayName("20~25세: 1,100,000원")
        void age20to25() {
            assertThat(service.calculateBasePremium(20)).isEqualTo(1_100_000);
            assertThat(service.calculateBasePremium(22)).isEqualTo(1_100_000);
            assertThat(service.calculateBasePremium(25)).isEqualTo(1_100_000);
        }

        @Test
        @DisplayName("26~30세: 900,000원")
        void age26to30() {
            assertThat(service.calculateBasePremium(26)).isEqualTo(900_000);
            assertThat(service.calculateBasePremium(28)).isEqualTo(900_000);
            assertThat(service.calculateBasePremium(30)).isEqualTo(900_000);
        }

        @Test
        @DisplayName("31~40세: 600,000원")
        void age31to40() {
            assertThat(service.calculateBasePremium(31)).isEqualTo(600_000);
            assertThat(service.calculateBasePremium(35)).isEqualTo(600_000);
            assertThat(service.calculateBasePremium(40)).isEqualTo(600_000);
        }

        @Test
        @DisplayName("41세 이상: 400,000원")
        void age41andAbove() {
            assertThat(service.calculateBasePremium(41)).isEqualTo(400_000);
            assertThat(service.calculateBasePremium(60)).isEqualTo(400_000);
        }

        @Test
        @DisplayName("경계값: 25세와 26세 구분")
        void boundaryAge25and26() {
            assertThat(service.calculateBasePremium(25)).isEqualTo(1_100_000);
            assertThat(service.calculateBasePremium(26)).isEqualTo(900_000);
        }

        @Test
        @DisplayName("경계값: 30세와 31세 구분")
        void boundaryAge30and31() {
            assertThat(service.calculateBasePremium(30)).isEqualTo(900_000);
            assertThat(service.calculateBasePremium(31)).isEqualTo(600_000);
        }

        @Test
        @DisplayName("경계값: 40세와 41세 구분")
        void boundaryAge40and41() {
            assertThat(service.calculateBasePremium(40)).isEqualTo(600_000);
            assertThat(service.calculateBasePremium(41)).isEqualTo(400_000);
        }
    }

    @Nested
    @DisplayName("calculateExperienceFactor - 운전 경력별 할증/할인 계수")
    class CalculateExperienceFactorTest {

        @Test
        @DisplayName("운전 경력 0년: 1.30 (30% 할증)")
        void experienceLessThan1Year() {
            assertThat(service.calculateExperienceFactor(0)).isEqualTo(1.30);
        }

        @Test
        @DisplayName("운전 경력 1년: 1.105")
        void experience1Year() {
            assertThat(service.calculateExperienceFactor(1)).isEqualTo(1.105);
        }

        @Test
        @DisplayName("운전 경력 2년: 1.105")
        void experience2Years() {
            assertThat(service.calculateExperienceFactor(2)).isEqualTo(1.105);
        }

        @Test
        @DisplayName("운전 경력 3년 이상: 1.00 (기본)")
        void experience3YearsAndAbove() {
            assertThat(service.calculateExperienceFactor(3)).isEqualTo(1.00);
            assertThat(service.calculateExperienceFactor(10)).isEqualTo(1.00);
        }

        @Test
        @DisplayName("경계값: 2년과 3년 구분")
        void boundary2and3Years() {
            assertThat(service.calculateExperienceFactor(2)).isEqualTo(1.105);
            assertThat(service.calculateExperienceFactor(3)).isEqualTo(1.00);
        }
    }

    @Nested
    @DisplayName("calculateScoreDiscountRate - 안전운전 점수 + 나이 조합 할인율")
    class CalculateScoreDiscountRateTest {

        @Test
        @DisplayName("점수 95 이상, 나이 28 이하: 26.5% 할인")
        void score95plusAgeUnder28() {
            assertThat(service.calculateScoreDiscountRate(25, 95)).isEqualTo(0.265);
            assertThat(service.calculateScoreDiscountRate(28, 100)).isEqualTo(0.265);
        }

        @Test
        @DisplayName("점수 95 이상, 나이 29 이상: 17.5% 할인")
        void score95plusAgeAbove28() {
            assertThat(service.calculateScoreDiscountRate(29, 95)).isEqualTo(0.175);
            assertThat(service.calculateScoreDiscountRate(40, 98)).isEqualTo(0.175);
        }

        @Test
        @DisplayName("점수 91~94, 나이 28 이하: 21.1% 할인")
        void score91to94AgeUnder28() {
            assertThat(service.calculateScoreDiscountRate(25, 91)).isEqualTo(0.211);
            assertThat(service.calculateScoreDiscountRate(28, 94)).isEqualTo(0.211);
        }

        @Test
        @DisplayName("점수 91~94, 나이 29 이상: 11.5% 할인")
        void score91to94AgeAbove28() {
            assertThat(service.calculateScoreDiscountRate(29, 91)).isEqualTo(0.115);
            assertThat(service.calculateScoreDiscountRate(50, 93)).isEqualTo(0.115);
        }

        @Test
        @DisplayName("점수 81~90, 나이 28 이하: 14.9% 할인")
        void score81to90AgeUnder28() {
            assertThat(service.calculateScoreDiscountRate(20, 81)).isEqualTo(0.149);
            assertThat(service.calculateScoreDiscountRate(28, 90)).isEqualTo(0.149);
        }

        @Test
        @DisplayName("점수 81~90, 나이 29 이상: 4.5% 할인")
        void score81to90AgeAbove28() {
            assertThat(service.calculateScoreDiscountRate(30, 81)).isEqualTo(0.045);
            assertThat(service.calculateScoreDiscountRate(45, 85)).isEqualTo(0.045);
        }

        @Test
        @DisplayName("점수 80 이하: 할인 없음 (0.0)")
        void scoreBelow81() {
            assertThat(service.calculateScoreDiscountRate(25, 80)).isEqualTo(0.0);
            assertThat(service.calculateScoreDiscountRate(25, 0)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("경계값: 나이 28과 29 구분 (점수 95)")
        void boundaryAge28and29WithHighScore() {
            assertThat(service.calculateScoreDiscountRate(28, 95)).isEqualTo(0.265);
            assertThat(service.calculateScoreDiscountRate(29, 95)).isEqualTo(0.175);
        }

        @Test
        @DisplayName("경계값: 점수 80과 81 구분")
        void boundaryScore80and81() {
            assertThat(service.calculateScoreDiscountRate(25, 80)).isEqualTo(0.0);
            assertThat(service.calculateScoreDiscountRate(25, 81)).isEqualTo(0.149);
        }
    }

    @Nested
    @DisplayName("calculateFinalPremium - 최종 보험료 계산")
    class CalculateFinalPremiumTest {

        @Test
        @DisplayName("경력 3년 이상, 할인율 없는 경우: 기본 보험료 그대로")
        void noDiscountNoSurcharge() {
            // age=35 -> base=600,000, experience=5 -> factor=1.0, score=50 -> discount=0.0
            int result = service.calculateFinalPremium(35, 50, 5);
            assertThat(result).isEqualTo(600_000);
        }

        @Test
        @DisplayName("경력 미만 1년, 점수 높음: 할증 후 할인 적용")
        void surchargeWithDiscount() {
            // age=25 -> base=1,100,000, experience=0 -> factor=1.30, score=95 -> discount=0.265
            int base = 1_100_000;
            double afterExperience = base * 1.30;
            double discountRate = 0.265;
            int expected = (int) (afterExperience * (1 - discountRate));

            int result = service.calculateFinalPremium(25, 95, 0);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("경력 1~2년, 점수 91점대: 할증 + 할인 조합")
        void experience1YearWithScore91() {
            // age=30 -> base=900,000, experience=2 -> factor=1.105, score=91 -> age>28 -> discount=0.115
            int base = 900_000;
            double afterExperience = base * 1.105;
            double discountRate = 0.115;
            int expected = (int) (afterExperience * (1 - discountRate));

            int result = service.calculateFinalPremium(30, 91, 2);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("41세 이상, 경력 3년 이상, 점수 81점대: 최저 구간 계산")
        void oldDriverWithModerateScore() {
            // age=45 -> base=400,000, experience=10 -> factor=1.0, score=85 -> age>28 -> discount=0.045
            int base = 400_000;
            double afterExperience = base * 1.0;
            double discountRate = 0.045;
            int expected = (int) (afterExperience * (1 - discountRate));

            int result = service.calculateFinalPremium(45, 85, 10);
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("최종 보험료는 양수여야 함")
        void finalPremiumIsPositive() {
            int result = service.calculateFinalPremium(35, 50, 5);
            assertThat(result).isPositive();
        }
    }
}