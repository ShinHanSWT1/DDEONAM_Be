package com.gorani.ecodrive.driving.service.insight;

import org.springframework.stereotype.Component;

@Component
public class DrivingInsightPromptFactory {

    public String systemPrompt() {
        return """
                너는 차량 주행 분석 코치다.
                입력된 수치 외 사실을 추측하지 마라.
                출력은 반드시 JSON이어야 한다.
                summary, insight, tip은 각 1문장으로 작성하라.
                문장은 짧고 자연스럽게 작성하라.
                서비스 화면에 바로 노출되는 문구이므로 딱딱한 보고서 문체는 피하라.
                사용자에게 설명하듯 부드럽고 간결한 한국어를 사용하라.
                비난하거나 공격적인 표현은 사용하지 마라.
                """;
    }

    public String userPrompt(DrivingInsightFeatures features, DrivingInsightClassification classification) {
        return """
                [고정 라벨]
                styleCode=%s
                styleLabel=%s

                [점수]
                safetyScore=%d
                efficiencyScore=%d
                stabilityScore=%d

                [핵심 지표]
                distanceKm=%.2f
                drivingMinutes=%d
                avgSpeed=%.2f
                maxSpeed=%.2f
                hardAccelPer100km=%.2f
                hardBrakePer100km=%.2f
                overspeedEventRatio=%.4f
                idlingRatio=%.4f
                nightDrivingRatio=%.4f

                [작성 규칙]
                - styleCode와 styleLabel은 그대로 유지하라.
                - 입력 피처 기반 문장만 작성하라.
                - overspeedEventRatio는 최근 7일 기준 세션 대비 과속 이벤트 비중이다.
                - overspeedEventRatio를 과속 시간 비율이나 과속 거리 비율로 해석하지 마라.
                - overspeedEventRatio를 문장에 쓸 때는 "과속 이벤트 비중" 또는 "세션 대비 과속 이벤트 비중"이라고만 표현하라.
                - "과속 비율", "과속 시간 비율", "과속 거리 비율" 같은 표현은 사용하지 마라.
                - summary는 운전 성향을 한눈에 이해할 수 있게 부드럽게 요약하라.
                - insight는 입력 지표에서 눈에 띄는 한 가지 관찰만 자연스럽게 설명하라.
                - tip은 바로 실천 가능한 개선 조언을 부담 없이 받아들일 수 있는 톤으로 작성하라.
                - 수치가 들어가면 의미가 바로 이해되도록 간단한 설명을 함께 붙여라.
                - "차량 주행 수치에서", "상당히 높습니다"처럼 딱딱하거나 과장된 표현은 피하라.
                - confidence는 0과 1 사이 숫자로 작성하라.
                """.formatted(
                classification.style().code(),
                classification.style().label(),
                classification.safetyScore(),
                classification.efficiencyScore(),
                classification.stabilityScore(),
                features.distanceKm().doubleValue(),
                features.drivingMinutes(),
                features.avgSpeed(),
                features.maxSpeed(),
                features.hardAccelPer100km(),
                features.hardBrakePer100km(),
                features.overspeedEventRatio(),
                features.idlingRatio(),
                features.nightDrivingRatio()
        );
    }
}
