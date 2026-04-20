package com.gorani.ecodrive.driving.service.insight;

public enum DrivingInsightStyle {
    STABLE_CRUISE("STABLE_CRUISE", "균형 주행형"),
    ECO_SAVER("ECO_SAVER", "절약 주행형"),
    COMMUTE_DENSE("COMMUTE_DENSE", "도심 적응형"),
    ACCEL_BIASED("ACCEL_BIASED", "페달 반응형"),
    BRAKE_HEAVY("BRAKE_HEAVY", "감속 민감형"),
    NEEDS_ATTENTION("NEEDS_ATTENTION", "속도 주의형"),
    INSUFFICIENT_DATA("INSUFFICIENT_DATA", "분석 대기");

    private final String code;
    private final String label;

    DrivingInsightStyle(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }
}
