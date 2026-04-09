package com.gorani.ecodrive.mission.service;

import com.gorani.ecodrive.mission.domain.MissionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 미션 기간 계산 유틸리티
 */
final class MissionPeriodSupport {

    private MissionPeriodSupport() {
    }

    /**
     * 미션 타입 기준 기간 계산 메서드
     */
    static MissionPeriod resolvePeriod(MissionType missionType, LocalDate baseDate) {
        if (missionType == MissionType.DAILY) {
            return new MissionPeriod(baseDate, baseDate);
        }
        LocalDate start = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = start.plusDays(6);
        return new MissionPeriod(start, end);
    }

    /**
     * 미션 기간 값 객체
     */
    record MissionPeriod(LocalDate startDate, LocalDate endDate) {
    }
}
