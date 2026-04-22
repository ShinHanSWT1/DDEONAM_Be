package com.gorani.ecodrive.mission.service;

import com.gorani.ecodrive.common.constants.TimeZoneConstants;
import com.gorani.ecodrive.driving.service.ingestion.DrivingIngestionService.UserDateKey;
import com.gorani.ecodrive.mission.domain.MissionStatus;
import com.gorani.ecodrive.mission.domain.MissionTargetType;
import com.gorani.ecodrive.mission.domain.MissionType;
import com.gorani.ecodrive.mission.domain.UserMission;
import com.gorani.ecodrive.mission.repository.UserMissionRepository;
import com.gorani.ecodrive.notification.event.MissionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissionProgressUpdateService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final UserMissionRepository userMissionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public int refreshProgress(List<UserDateKey> affectedUserDates) {
        if (affectedUserDates == null || affectedUserDates.isEmpty()) {
            return 0;
        }

        // 주행 반영으로 영향받은 날짜를 일/주/월 미션 기간 키로 확장
        Set<PeriodKey> periodKeys = resolvePeriodKeys(affectedUserDates);
        int updatedMissionCount = 0;

        for (PeriodKey periodKey : periodKeys) {
            List<UserMission> missions = userMissionRepository.findAssignedMissions(
                    periodKey.userId(),
                    periodKey.missionType(),
                    periodKey.periodStartDate()
            );
            if (missions.isEmpty()) {
                continue;
            }

            PeriodMetrics metrics = loadPeriodMetrics(
                    periodKey.userId(),
                    periodKey.periodStartDate(),
                    periodKey.periodEndDate()
            );

            for (UserMission mission : missions) {
                // 목표 타입별 현재값을 계산하고 달성률/완료 여부를 한 번에 반영
                BigDecimal currentValue = resolveCurrentValue(mission.getTargetTypeSnapshot(), metrics);
                BigDecimal progressRate = calculateProgressRate(
                        mission.getTargetTypeSnapshot(),
                        currentValue,
                        mission.getTargetValueSnapshot()
                );
                boolean achieved = isAchieved(
                        mission.getMissionTypeSnapshot(),
                        mission.getPeriodEndDate(),
                        mission.getTargetTypeSnapshot(),
                        currentValue,
                        mission.getTargetValueSnapshot()
                );
                boolean wasInProgress = mission.getStatus() == MissionStatus.IN_PROGRESS;
                mission.applyProgress(currentValue, progressRate, achieved);
                if (wasInProgress && mission.getStatus() == MissionStatus.COMPLETED) {
                    eventPublisher.publishEvent(
                            new MissionCompletedEvent(
                                    mission.getUser().getId(),
                                    mission.getMissionPolicy().getTitle()
                            )
                    );
                }
                updatedMissionCount++;
            }
        }

        log.info("Mission progress refreshed from driving data. periods={}, updatedMissions={}", periodKeys.size(), updatedMissionCount);
        return updatedMissionCount;
    }

    private Set<PeriodKey> resolvePeriodKeys(List<UserDateKey> affectedUserDates) {
        Set<PeriodKey> keys = new LinkedHashSet<>();
        for (UserDateKey key : affectedUserDates) {
            LocalDate date = key.sessionDate();

            // DAILY는 해당 일자 1일만 계산
            keys.add(new PeriodKey(
                    key.userId(),
                    MissionType.DAILY,
                    date,
                    date
            ));

            // WEEKLY는 같은 주(월~일) 전체 구간으로 계산
            MissionPeriodSupport.MissionPeriod weeklyPeriod = MissionPeriodSupport.resolvePeriod(MissionType.WEEKLY, date);
            keys.add(new PeriodKey(
                    key.userId(),
                    MissionType.WEEKLY,
                    weeklyPeriod.startDate(),
                    weeklyPeriod.endDate()
            ));

            // MONTHLY는 해당 월(1일~말일) 전체 구간으로 계산
            MissionPeriodSupport.MissionPeriod monthlyPeriod = MissionPeriodSupport.resolvePeriod(MissionType.MONTHLY, date);
            keys.add(new PeriodKey(
                    key.userId(),
                    MissionType.MONTHLY,
                    monthlyPeriod.startDate(),
                    monthlyPeriod.endDate()
            ));
        }
        return keys;
    }

    private PeriodMetrics loadPeriodMetrics(Long userId, LocalDate startDate, LocalDate endDate) {
        // 주행 세션 기반 합계 지표(거리/공회전/야간주행 건수)를 기간 단위로 집계
        SessionMetrics sessionMetrics = jdbcTemplate.query("""
                        select
                            count(*) as total_sessions,
                            coalesce(sum(distance_km), 0) as total_distance_km,
                            coalesce(sum(idling_time_minutes), 0) as total_idling_minutes,
                            coalesce(sum(case
                                when extract(hour from started_at) >= 22
                                  or extract(hour from started_at) < 6
                                  or extract(hour from ended_at) >= 22
                                  or extract(hour from ended_at) < 6
                                then 1 else 0 end), 0) as night_sessions
                        from driving_sessions
                        where user_id = ?
                          and session_date between ? and ?
                        """,
                rs -> rs.next()
                        ? new SessionMetrics(
                        rs.getInt("total_sessions"),
                        rs.getBigDecimal("total_distance_km"),
                        rs.getInt("total_idling_minutes"),
                        rs.getInt("night_sessions")
                )
                        : new SessionMetrics(0, ZERO, 0, 0),
                userId,
                startDate,
                endDate
        );

        EventMetrics eventMetrics = jdbcTemplate.query("""
                        select
                            coalesce(sum(case when e.event_type = 'RAPID_ACCEL' then 1 else 0 end), 0) as rapid_accel_count,
                            coalesce(sum(case when e.event_type = 'HARD_BRAKE' then 1 else 0 end), 0) as hard_brake_count
                        from driving_events e
                        join driving_sessions s on e.driving_session_id = s.id
                        where e.user_id = ?
                          and s.session_date between ? and ?
                        """,
                rs -> rs.next()
                        ? new EventMetrics(
                        rs.getInt("rapid_accel_count"),
                        rs.getInt("hard_brake_count")
                )
                        : new EventMetrics(0, 0),
                userId,
                startDate,
                endDate
        );

        // SAFE_SCORE_GTE는 동일 기간의 최신 스냅샷 점수를 사용
        Integer safeScore = jdbcTemplate.query("""
                        select score
                        from driving_score_snapshots
                        where user_id = ?
                          and snapshot_date between ? and ?
                        order by snapshot_date desc, id desc
                        limit 1
                        """,
                rs -> rs.next() ? rs.getInt("score") : null,
                userId,
                startDate,
                endDate
        );

        // NO_HARD_EVENT_DAYS_GTE는 하드 이벤트가 0인 '일(day)'의 개수를 계산
        Integer noHardEventDays = jdbcTemplate.query("""
                        select count(*) as no_hard_event_days
                        from (
                            select
                                s.session_date,
                                coalesce(sum(case
                                    when e.event_type in ('RAPID_ACCEL', 'HARD_BRAKE', 'OVERSPEED')
                                    then 1 else 0 end), 0) as hard_event_count
                            from driving_sessions s
                            left join driving_events e on e.driving_session_id = s.id
                            where s.user_id = ?
                              and s.session_date between ? and ?
                            group by s.session_date
                        ) daily
                        where daily.hard_event_count = 0
                        """,
                rs -> rs.next() ? rs.getInt("no_hard_event_days") : 0,
                userId,
                startDate,
                endDate
        );

        return new PeriodMetrics(
                sessionMetrics.totalDistanceKm(),
                BigDecimal.valueOf(safeScore == null ? 0 : safeScore),
                BigDecimal.valueOf(eventMetrics.rapidAccelCount()),
                BigDecimal.valueOf(eventMetrics.hardBrakeCount()),
                BigDecimal.valueOf(sessionMetrics.totalIdlingMinutes()),
                calculateNightDriveRatio(sessionMetrics.nightSessions(), sessionMetrics.totalSessions()),
                BigDecimal.valueOf(noHardEventDays == null ? 0 : noHardEventDays)
        );
    }

    private BigDecimal resolveCurrentValue(MissionTargetType targetType, PeriodMetrics metrics) {
        return switch (targetType) {
            case DISTANCE_KM_GTE -> metrics.distanceKm();
            case SAFE_SCORE_GTE -> metrics.safeScore();
            case HARD_ACCEL_COUNT_LTE -> metrics.hardAccelCount();
            case HARD_BRAKE_COUNT_LTE -> metrics.hardBrakeCount();
            case IDLING_MINUTES_LTE -> metrics.idlingMinutes();
            case NIGHT_DRIVE_RATIO_LTE -> metrics.nightDriveRatio();
            case NO_HARD_EVENT_DAYS_GTE -> metrics.noHardEventDays();
        };
    }

    private boolean isAchieved(
            MissionType missionType,
            LocalDate periodEndDate,
            MissionTargetType targetType,
            BigDecimal currentValue,
            BigDecimal targetValue
    ) {
        if (targetValue == null) {
            return false;
        }

        // 안전점수 유지형(주간/월간)은 기간 종료일에만 완료 확정한다.
        if (targetType == MissionTargetType.SAFE_SCORE_GTE && missionType != MissionType.DAILY) {
            LocalDate today = LocalDate.now(TimeZoneConstants.KST);
            if (today.isBefore(periodEndDate)) {
                return false;
            }
        }

        return isGteTarget(targetType)
                ? currentValue.compareTo(targetValue) >= 0
                : currentValue.compareTo(targetValue) <= 0;
    }

    private BigDecimal calculateProgressRate(
            MissionTargetType targetType,
            BigDecimal currentValue,
            BigDecimal targetValue
    ) {
        if (targetValue == null || targetValue.compareTo(ZERO) <= 0) {
            return ZERO;
        }

        BigDecimal ratio;
        if (isGteTarget(targetType)) {
            ratio = currentValue.divide(targetValue, 6, RoundingMode.HALF_UP);
        } else {
            // LTE 타입은 값이 낮을수록 유리하므로 역비율(target/current)로 진행률을 계산
            if (currentValue.compareTo(ZERO) <= 0) {
                return HUNDRED;
            }
            ratio = targetValue.divide(currentValue, 6, RoundingMode.HALF_UP);
        }

        BigDecimal percent = ratio.multiply(HUNDRED);
        if (percent.compareTo(HUNDRED) > 0) {
            return HUNDRED;
        }
        if (percent.compareTo(ZERO) < 0) {
            return ZERO;
        }
        return percent.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isGteTarget(MissionTargetType targetType) {
        return targetType == MissionTargetType.DISTANCE_KM_GTE
                || targetType == MissionTargetType.SAFE_SCORE_GTE
                || targetType == MissionTargetType.NO_HARD_EVENT_DAYS_GTE;
    }

    private BigDecimal calculateNightDriveRatio(int nightSessions, int totalSessions) {
        if (totalSessions <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(nightSessions)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(totalSessions), 2, RoundingMode.HALF_UP);
    }

    private record PeriodKey(
            Long userId,
            MissionType missionType,
            LocalDate periodStartDate,
            LocalDate periodEndDate
    ) {
    }

    private record SessionMetrics(
            int totalSessions,
            BigDecimal totalDistanceKm,
            int totalIdlingMinutes,
            int nightSessions
    ) {
    }

    private record EventMetrics(
            int rapidAccelCount,
            int hardBrakeCount
    ) {
    }

    private record PeriodMetrics(
            BigDecimal distanceKm,
            BigDecimal safeScore,
            BigDecimal hardAccelCount,
            BigDecimal hardBrakeCount,
            BigDecimal idlingMinutes,
            BigDecimal nightDriveRatio,
            BigDecimal noHardEventDays
    ) {
    }
}
