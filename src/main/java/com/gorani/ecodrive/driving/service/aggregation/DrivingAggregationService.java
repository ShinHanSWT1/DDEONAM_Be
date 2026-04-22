package com.gorani.ecodrive.driving.service.aggregation;

import com.gorani.ecodrive.driving.service.ingestion.DrivingIngestionService.UserDateKey;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DrivingAggregationService {

    private static final int DEFAULT_BASE_SCORE = 100;
    private static final int MAX_PENALTY = 45;
    private static final int SAFETY_RAPID_ACCEL_WEIGHT = 4;
    private static final int SAFETY_HARD_BRAKE_WEIGHT = 5;
    private static final int SAFETY_OVERSPEED_WEIGHT = 6;
    private static final int ECO_IDLING_RATIO_WEIGHT = 50;
    private static final int ECO_RAPID_ACCEL_WEIGHT = 5;
    private static final int ECO_HARD_BRAKE_WEIGHT = 3;

    private static final BigDecimal BASELINE_DISTANCE_EMISSION_FACTOR = BigDecimal.valueOf(160);
    private static final BigDecimal BASELINE_IDLING_FACTOR = BigDecimal.valueOf(25);
    private static final BigDecimal BASELINE_ACCEL_FACTOR = BigDecimal.valueOf(18);
    private static final BigDecimal BASELINE_DECEL_FACTOR = BigDecimal.valueOf(12);
    private static final BigDecimal REWARD_POINT_FACTOR = BigDecimal.valueOf(100);
    private static final BigDecimal GRAM_TO_KILOGRAM = BigDecimal.valueOf(1000);

    private final JdbcTemplate jdbcTemplate;

    public int refreshSummaries(List<UserDateKey> affectedUserDates) {
        Set<UserDateKey> datesToRefresh = expandAffectedDates(affectedUserDates);
        int updatedUsers = 0;
        for (UserDateKey key : datesToRefresh) {
            AggregatedDrivingMetrics metrics = loadMetrics(key.userId(), key.userVehicleId(), key.sessionDate());
            if (metrics == null) {
                continue;
            }

            upsertSafetySnapshot(key.userId(), key.userVehicleId(), key.sessionDate(), metrics.safetyScore());
            upsertCarbonSnapshot(key.userId(), key.userVehicleId(), key.sessionDate(), metrics.carbonReductionKg(), metrics.rewardPoint());
            upsertScoreChangeLog(key.userId(), key.userVehicleId(), key.sessionDate(), metrics.safetyScore());
            updatedUsers++;
        }
        return updatedUsers;
    }

    private Set<UserDateKey> expandAffectedDates(List<UserDateKey> affectedUserDates) {
        Set<UserDateKey> expanded = new LinkedHashSet<>();
        for (UserDateKey key : affectedUserDates) {
            LocalDate monthStart = key.sessionDate().withDayOfMonth(1);
            YearMonth yearMonth = YearMonth.from(key.sessionDate());
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            List<LocalDate> impactedDates = jdbcTemplate.query("""
                            select distinct session_date
                            from driving_sessions
                            where user_id = ?
                              and user_vehicle_id = ?
                              and session_date between ? and ?
                              and session_date >= ?
                            order by session_date asc
                            """,
                    (rs, rowNum) -> rs.getObject("session_date", LocalDate.class),
                    key.userId(),
                    key.userVehicleId(),
                    monthStart,
                    monthEnd,
                    key.sessionDate()
            );

            if (impactedDates.isEmpty()) {
                expanded.add(key);
                continue;
            }

            for (LocalDate impactedDate : impactedDates) {
                expanded.add(new UserDateKey(key.userId(), key.userVehicleId(), impactedDate));
            }
        }
        return expanded;
    }

    private AggregatedDrivingMetrics loadMetrics(Long userId, Long userVehicleId, LocalDate sessionDate) {
        LocalDate monthStart = sessionDate.withDayOfMonth(1);
        SessionMetrics sessionMetrics = jdbcTemplate.query("""
                        select
                            count(*) as session_count,
                            coalesce(sum(distance_km), 0) as total_distance_km,
                            coalesce(sum(driving_time_minutes), 0) as total_driving_time_minutes,
                            coalesce(sum(idling_time_minutes), 0) as total_idling_time_minutes
                        from driving_sessions
                        where user_id = ?
                          and user_vehicle_id = ?
                          and session_date between ? and ?
                        """,
                rs -> rs.next()
                        ? new SessionMetrics(
                        rs.getInt("session_count"),
                        rs.getBigDecimal("total_distance_km"),
                        rs.getInt("total_driving_time_minutes"),
                        rs.getInt("total_idling_time_minutes")
                        )
                        : null,
                userId,
                userVehicleId,
                monthStart,
                sessionDate
        );

        if (sessionMetrics == null || sessionMetrics.sessionCount() == 0) {
            return null;
        }

        EventMetrics eventMetrics = jdbcTemplate.query("""
                        select
                            coalesce(sum(case when event_type = 'RAPID_ACCEL' then 1 else 0 end), 0) as rapid_accel_count,
                            coalesce(sum(case when event_type = 'HARD_BRAKE' then 1 else 0 end), 0) as hard_brake_count,
                            coalesce(sum(case when event_type = 'OVERSPEED' then 1 else 0 end), 0) as overspeed_count
                        from driving_events e
                        join driving_sessions s on e.driving_session_id = s.id
                        where e.user_id = ?
                          and s.user_vehicle_id = ?
                          and s.session_date between ? and ?
                        """,
                rs -> rs.next()
                        ? new EventMetrics(
                        rs.getInt("rapid_accel_count"),
                        rs.getInt("hard_brake_count"),
                        rs.getInt("overspeed_count")
                )
                        : new EventMetrics(0, 0, 0),
                userId,
                userVehicleId,
                monthStart,
                sessionDate
        );

        VehicleProfile vehicleProfile = loadVehicleProfile(userVehicleId);

        int sessionCount = Math.max(sessionMetrics.sessionCount(), 1);
        double rapidAccelPerSession = (double) eventMetrics.rapidAccelCount() / sessionCount;
        double hardBrakePerSession = (double) eventMetrics.hardBrakeCount() / sessionCount;
        double overspeedPerSession = (double) eventMetrics.overspeedCount() / sessionCount;

        double idleRatio = sessionMetrics.totalDrivingTimeMinutes() == 0
                ? 0
                : (double) sessionMetrics.totalIdlingTimeMinutes() / sessionMetrics.totalDrivingTimeMinutes();

        int safetyScore = calculateSafetyScore(rapidAccelPerSession, hardBrakePerSession, overspeedPerSession);
        int ecoScore = calculateEcoScore(idleRatio, rapidAccelPerSession, hardBrakePerSession);
        BigDecimal carbonReductionKg = calculateCarbonReductionKg(sessionMetrics, eventMetrics, vehicleProfile);
        int rewardPoint = calculateRewardPoint(carbonReductionKg);

        return new AggregatedDrivingMetrics(safetyScore, ecoScore, carbonReductionKg, rewardPoint);
    }

    private int calculateSafetyScore(
            double rapidAccelPerSession,
            double hardBrakePerSession,
            double overspeedPerSession
    ) {
        int safetyPenalty = (int) Math.round(
                (rapidAccelPerSession * SAFETY_RAPID_ACCEL_WEIGHT)
                        + (hardBrakePerSession * SAFETY_HARD_BRAKE_WEIGHT)
                        + (overspeedPerSession * SAFETY_OVERSPEED_WEIGHT)
        );
        return clamp(DEFAULT_BASE_SCORE - Math.min(safetyPenalty, MAX_PENALTY));
    }

    private int calculateEcoScore(double idleRatio, double rapidAccelPerSession, double hardBrakePerSession) {
        int ecoPenalty = (int) Math.round(
                (idleRatio * ECO_IDLING_RATIO_WEIGHT)
                        + (rapidAccelPerSession * ECO_RAPID_ACCEL_WEIGHT)
                        + (hardBrakePerSession * ECO_HARD_BRAKE_WEIGHT)
        );
        return clamp(DEFAULT_BASE_SCORE - Math.min(ecoPenalty, MAX_PENALTY));
    }

    private BigDecimal calculateCarbonReductionKg(
            SessionMetrics sessionMetrics,
            EventMetrics eventMetrics,
            VehicleProfile vehicleProfile
    ) {
        BigDecimal actualCo2G = sessionMetrics.totalDistanceKm()
                .multiply(vehicleProfile.baseEmissionFactor())
                .multiply(vehicleProfile.vehicleSizeFactor())
                .add(BigDecimal.valueOf(sessionMetrics.totalIdlingTimeMinutes()).multiply(vehicleProfile.idleFactor()))
                .add(BigDecimal.valueOf(eventMetrics.rapidAccelCount()).multiply(vehicleProfile.accelFactor()))
                .add(BigDecimal.valueOf(eventMetrics.hardBrakeCount()).multiply(vehicleProfile.decelFactor()));

        BigDecimal baselineCo2G = sessionMetrics.totalDistanceKm()
                .multiply(BASELINE_DISTANCE_EMISSION_FACTOR)
                .multiply(vehicleProfile.vehicleSizeFactor())
                .add(BigDecimal.valueOf(sessionMetrics.totalIdlingTimeMinutes()).multiply(BASELINE_IDLING_FACTOR))
                .add(BigDecimal.valueOf(eventMetrics.rapidAccelCount()).multiply(BASELINE_ACCEL_FACTOR))
                .add(BigDecimal.valueOf(eventMetrics.hardBrakeCount()).multiply(BASELINE_DECEL_FACTOR));

        return baselineCo2G.subtract(actualCo2G).max(BigDecimal.ZERO)
                .divide(GRAM_TO_KILOGRAM, 4, RoundingMode.HALF_UP);
    }

    private int calculateRewardPoint(BigDecimal carbonReductionKg) {
        return carbonReductionKg.multiply(REWARD_POINT_FACTOR)
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }

    private VehicleProfile loadVehicleProfile(Long userVehicleId) {
        return jdbcTemplate.query("""
                        select vm.fuel_type, vm.body_type
                        from user_vehicles uv
                        join vehicle_models vm on uv.vehicle_model_id = vm.id
                        where uv.id = ?
                        """,
                rs -> rs.next()
                        ? toVehicleProfile(rs.getString("fuel_type"), rs.getString("body_type"))
                        : new VehicleProfile(
                        BigDecimal.valueOf(150),
                        BigDecimal.ONE,
                        BigDecimal.valueOf(20),
                        BigDecimal.valueOf(15),
                        BigDecimal.valueOf(10)
                ),
                userVehicleId
        );
    }

    private VehicleProfile toVehicleProfile(String fuelType, String bodyType) {
        String normalizedFuelType = fuelType == null ? "GASOLINE" : fuelType.toUpperCase(Locale.ROOT);
        BigDecimal baseEmissionFactor = switch (normalizedFuelType) {
            case "DIESEL" -> BigDecimal.valueOf(130);
            case "HYBRID" -> BigDecimal.valueOf(90);
            default -> BigDecimal.valueOf(150);
        };

        BigDecimal idleFactor = switch (normalizedFuelType) {
            case "DIESEL" -> BigDecimal.valueOf(25);
            case "HYBRID" -> BigDecimal.valueOf(10);
            default -> BigDecimal.valueOf(20);
        };

        String normalizedBodyType = bodyType == null ? "" : bodyType.toUpperCase(Locale.ROOT);
        boolean isSmall = normalizedBodyType.contains("SMALL") || bodyTypeContains(bodyType, "경", "소형", "compact");
        boolean isLarge = normalizedBodyType.contains("LARGE") || bodyTypeContains(bodyType, "대형", "SUV", "승합");

        BigDecimal vehicleSizeFactor = isSmall
                ? BigDecimal.valueOf(0.8)
                : isLarge ? BigDecimal.valueOf(1.2) : BigDecimal.ONE;

        BigDecimal accelFactor;
        BigDecimal decelFactor;

        if ("HYBRID".equals(normalizedFuelType)) {
            accelFactor = BigDecimal.valueOf(8);
        } else if (isSmall) {
            accelFactor = BigDecimal.valueOf(10);
        } else if (isLarge) {
            accelFactor = BigDecimal.valueOf(20);
        } else {
            accelFactor = BigDecimal.valueOf(15);
        }

        if (isSmall) {
            decelFactor = BigDecimal.valueOf(7);
        } else if (isLarge) {
            decelFactor = BigDecimal.valueOf(12);
        } else {
            decelFactor = BigDecimal.valueOf(10);
        }

        return new VehicleProfile(baseEmissionFactor, vehicleSizeFactor, idleFactor, accelFactor, decelFactor);
    }

    private boolean bodyTypeContains(String bodyType, String... keywords) {
        if (bodyType == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (bodyType.toUpperCase(Locale.ROOT).contains(keyword.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void upsertSafetySnapshot(Long userId, Long userVehicleId, LocalDate sessionDate, int safetyScore) {
        Long snapshotId = jdbcTemplate.query("""
                        select id
                        from driving_score_snapshots
                        where user_vehicle_id = ? and snapshot_date = ?
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userVehicleId,
                sessionDate
        );

        if (snapshotId == null) {
            jdbcTemplate.update("""
                            insert into driving_score_snapshots (user_id, user_vehicle_id, snapshot_date, score, created_at)
                            values (?, ?, ?, ?, ?)
                            """,
                    userId,
                    userVehicleId,
                    Date.valueOf(sessionDate),
                    safetyScore,
                    Timestamp.valueOf(LocalDateTime.now())
            );
            return;
        }

        jdbcTemplate.update("""
                        update driving_score_snapshots
                        set score = ?
                        where id = ?
                        """,
                safetyScore,
                snapshotId
        );
    }

    private void upsertCarbonSnapshot(Long userId, Long userVehicleId, LocalDate sessionDate, BigDecimal carbonReductionKg, int rewardPoint) {
        Long snapshotId = jdbcTemplate.query("""
                        select id
                        from carbon_reduction_snapshots
                        where user_vehicle_id = ? and snapshot_date = ?
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userVehicleId,
                sessionDate
        );

        if (snapshotId == null) {
            jdbcTemplate.update("""
                            insert into carbon_reduction_snapshots (
                                user_id,
                                user_vehicle_id,
                                snapshot_date,
                                carbon_reduction_kg,
                                reward_point,
                                created_at
                            ) values (?, ?, ?, ?, ?, ?)
                            """,
                    userId,
                    userVehicleId,
                    Date.valueOf(sessionDate),
                    carbonReductionKg,
                    rewardPoint,
                    Timestamp.valueOf(LocalDateTime.now())
            );
            return;
        }

        jdbcTemplate.update("""
                        update carbon_reduction_snapshots
                        set carbon_reduction_kg = ?, reward_point = ?
                        where id = ?
                        """,
                carbonReductionKg,
                rewardPoint,
                snapshotId
        );
    }

    private void upsertScoreChangeLog(Long userId, Long userVehicleId, LocalDate sessionDate, int safetyScore) {
        Long snapshotId = jdbcTemplate.query("""
                        select id
                        from driving_score_snapshots
                        where user_vehicle_id = ? and snapshot_date = ?
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userVehicleId,
                sessionDate
        );

        if (snapshotId == null) {
            return;
        }

        Integer previousScore = jdbcTemplate.query("""
                        select score
                        from driving_score_snapshots
                        where user_vehicle_id = ? and snapshot_date < ?
                          and date_trunc('month', snapshot_date) = date_trunc('month', ?::date)
                        order by snapshot_date desc
                        limit 1
                        """,
                rs -> rs.next() ? rs.getInt("score") : null,
                userVehicleId,
                sessionDate,
                Date.valueOf(sessionDate)
        );

        int scoreDelta = previousScore == null ? safetyScore - DEFAULT_BASE_SCORE : safetyScore - previousScore;

        jdbcTemplate.update("""
                        delete from driving_score_change_logs
                        where snapshot_id = ? and change_type = 'DUMMY_REFRESH'
                        """,
                snapshotId
        );

        jdbcTemplate.update("""
                        insert into driving_score_change_logs (
                            user_id,
                            snapshot_id,
                            change_date,
                            change_type,
                            message,
                            score_delta,
                            display_order,
                            created_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                snapshotId,
                Date.valueOf(sessionDate),
                "DUMMY_REFRESH",
                "더미 주행데이터 반영으로 점수가 갱신되었습니다.",
                scoreDelta,
                1,
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private record SessionMetrics(
            int sessionCount,
            BigDecimal totalDistanceKm,
            int totalDrivingTimeMinutes,
            int totalIdlingTimeMinutes
    ) {
    }

    private record EventMetrics(
            int rapidAccelCount,
            int hardBrakeCount,
            int overspeedCount
    ) {
    }

    private record VehicleProfile(
            BigDecimal baseEmissionFactor,
            BigDecimal vehicleSizeFactor,
            BigDecimal idleFactor,
            BigDecimal accelFactor,
            BigDecimal decelFactor
    ) {
    }

    private record AggregatedDrivingMetrics(
            int safetyScore,
            int ecoScore,
            BigDecimal carbonReductionKg,
            int rewardPoint
    ) {
    }
}
