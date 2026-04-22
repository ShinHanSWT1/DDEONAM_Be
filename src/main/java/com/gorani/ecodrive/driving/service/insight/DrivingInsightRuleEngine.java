package com.gorani.ecodrive.driving.service.insight;

import com.gorani.ecodrive.driving.dto.query.DrivingInsightResponse;
import org.springframework.stereotype.Component;

@Component
public class DrivingInsightRuleEngine {

    public DrivingInsightClassification classify(DrivingInsightFeatures features) {
        if (features.isInsufficientForInsight()) {
            return new DrivingInsightClassification(DrivingInsightStyle.INSUFFICIENT_DATA, 0, 0, 0);
        }

        int safetyScore = clamp(100
                - (int) Math.round(features.overspeedEventRatio() * 55)
                - (int) Math.round(features.hardBrakePer100km() * 3.2)
                - (int) Math.round(features.hardAccelPer100km() * 2.8));
        int efficiencyScore = clamp(100
                - (int) Math.round(features.idlingRatio() * 70)
                - (int) Math.round(features.hardAccelPer100km() * 3.0)
                - (int) Math.round(features.hardBrakePer100km() * 1.8));
        int stabilityScore = clamp(100
                - (int) Math.round(Math.abs(features.hardBrakePer100km() - features.hardAccelPer100km()) * 5)
                - (int) Math.round(features.maxSpeed() >= 125 ? 8 : 0)
                - (int) Math.round(features.nightDrivingRatio() * 12));

        if (features.overspeedEventRatio() >= 0.18
                || (features.maxSpeed() >= 125 && features.overspeedEventRatio() >= 0.12)
                || (features.hardBrakePer100km() >= 7.0 && features.hardAccelPer100km() >= 7.0)) {
            return new DrivingInsightClassification(DrivingInsightStyle.NEEDS_ATTENTION, safetyScore, efficiencyScore, stabilityScore);
        }

        if (features.hardBrakePer100km() >= 5.5
                && features.hardBrakePer100km() - features.hardAccelPer100km() >= 1.5
                && features.overspeedEventRatio() < 0.18) {
            return new DrivingInsightClassification(DrivingInsightStyle.BRAKE_HEAVY, safetyScore, efficiencyScore, stabilityScore);
        }

        if (features.hardAccelPer100km() >= 5.5
                && features.hardAccelPer100km() - features.hardBrakePer100km() >= 1.5
                && features.overspeedEventRatio() < 0.18) {
            return new DrivingInsightClassification(DrivingInsightStyle.ACCEL_BIASED, safetyScore, efficiencyScore, stabilityScore);
        }

        if (features.avgSpeed() <= 28
                && features.idlingRatio() >= 0.20
                && features.overspeedEventRatio() < 0.10) {
            return new DrivingInsightClassification(DrivingInsightStyle.COMMUTE_DENSE, safetyScore, efficiencyScore, stabilityScore);
        }

        if (features.idlingRatio() < 0.10
                && features.hardAccelPer100km() < 3.5
                && features.hardBrakePer100km() < 3.5
                && features.overspeedEventRatio() < 0.05
                && features.avgSpeed() >= 25) {
            return new DrivingInsightClassification(DrivingInsightStyle.ECO_SAVER, safetyScore, efficiencyScore, stabilityScore);
        }

        return new DrivingInsightClassification(DrivingInsightStyle.STABLE_CRUISE, safetyScore, efficiencyScore, stabilityScore);
    }

    public DrivingInsightResponse fallbackResponse(DrivingInsightClassification classification, String version, boolean fallbackUsed) {
        return switch (classification.style()) {
            case NEEDS_ATTENTION -> new DrivingInsightResponse(
                    classification.style().code(),
                    classification.style().label(),
                    "위험 지표가 높아 주의가 필요한 주행 패턴입니다.",
                    "과속 이벤트 비중이나 급가감속 빈도가 다른 지표보다 높게 나타났습니다.",
                    "속도를 조금만 낮추고 앞차와의 간격을 더 확보해 보세요.",
                    0.72,
                    version,
                    fallbackUsed
            );
            case BRAKE_HEAVY -> new DrivingInsightResponse(
                    classification.style().code(),
                    classification.style().label(),
                    "감속 이벤트 비율이 높아 흐름이 자주 끊기는 편입니다.",
                    "최근 7일 기준 급감속 빈도가 급가속보다 높게 나타났습니다.",
                    "앞차 간격을 조금 더 여유 있게 두면 급감속 빈도를 줄일 수 있습니다.",
                    0.76,
                    version,
                    fallbackUsed
            );
            case ACCEL_BIASED -> new DrivingInsightResponse(
                    classification.style().code(),
                    classification.style().label(),
                    "가속 이벤트 비율이 높아 에너지 손실이 커질 수 있습니다.",
                    "최근 7일 기준 급가속 빈도가 급감속보다 높게 나타났습니다.",
                    "출발과 재가속 구간에서 페달 입력을 조금만 부드럽게 해 보세요.",
                    0.76,
                    version,
                    fallbackUsed
            );
            case COMMUTE_DENSE -> new DrivingInsightResponse(
                    classification.style().code(),
                    classification.style().label(),
                    "저속 구간과 공회전 비율이 높아 정체 영향이 큰 주행 패턴입니다.",
                    "평균 속도는 낮고 공회전 비율은 높아 도심 정체 패턴이 두드러집니다.",
                    "짧은 정차가 반복되는 구간에서는 급출발을 줄이는 쪽이 유리합니다.",
                    0.78,
                    version,
                    fallbackUsed
            );
            case ECO_SAVER -> new DrivingInsightResponse(
                    classification.style().code(),
                    classification.style().label(),
                    "불필요한 가감속과 공회전이 적은 효율적인 주행 패턴입니다.",
                    "공회전과 급가감속 지표가 모두 낮아 효율성이 안정적으로 유지되고 있습니다.",
                    "지금의 부드러운 페달 조작을 유지하면 효율 지표를 계속 지킬 수 있습니다.",
                    0.81,
                    version,
                    fallbackUsed
            );
            case INSUFFICIENT_DATA -> new DrivingInsightResponse(
                    classification.style().code(),
                    classification.style().label(),
                    "데이터가 더 필요합니다.",
                    "최근 7일 주행 기록이 충분하지 않아 스타일 분석을 아직 확정하기 어렵습니다.",
                    "조금 더 주행한 뒤 다시 확인하면 더 정확한 인사이트를 받을 수 있습니다.",
                    0.0,
                    version,
                    false
            );
            default -> new DrivingInsightResponse(
                    classification.style().code(),
                    classification.style().label(),
                    "전반적인 지표가 고르게 안정적인 주행 패턴입니다.",
                    "급가감속과 과속 이벤트 지표가 특정 방향으로 크게 치우치지 않았습니다.",
                    "지금의 주행 리듬을 유지하면서 공회전만 더 줄여 보세요.",
                    0.74,
                    version,
                    fallbackUsed
            );
        };
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(value, 100));
    }
}
