package com.gorani.ecodrive.mission.domain;

/**
 * 미션 목표 판정 타입 enum
 */
public enum MissionTargetType {
    DISTANCE_KM_GTE,
    SAFE_SCORE_GTE,
    HARD_ACCEL_COUNT_LTE,
    HARD_BRAKE_COUNT_LTE,
    IDLING_MINUTES_LTE,
    NIGHT_DRIVE_RATIO_LTE,
    NO_HARD_EVENT_DAYS_GTE
}
