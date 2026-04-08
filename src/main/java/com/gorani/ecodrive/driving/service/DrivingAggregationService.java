package com.gorani.ecodrive.driving.service;

import com.gorani.ecodrive.driving.service.DrivingIngestionService.UserDateKey;
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

    private final JdbcTemplate jdbcTemplate;

    public int refreshSummaries(List<UserDateKey> affectedUserDates) {
        Set<UserDateKey> datesToRefresh = expandAffectedDates(affectedUserDates);
        int updatedUsers = 0;
        for (UserDateKey key : datesToRefresh) {
            AggregatedDrivingMetrics metrics = loadMetrics(key.userId(), key.sessionDate());
            if (metrics == null) {
                continue;
            }

            upsertSafetySnapshot(key.userId(), key.sessionDate(), metrics.safetyScore());
            upsertCarbonSnapshot(key.userId(), key.sessionDate(), metrics.carbonReductionKg(), metrics.rewardPoint());
            upsertScoreChangeLog(key.userId(), key.sessionDate(), metrics.safetyScore());
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
                              and session_date between ? and ?
                              and session_date >= ?
                            order by session_date asc
                            """,
                    (rs, rowNum) -> rs.getObject("session_date", LocalDate.class),
                    key.userId(),
                    monthStart,
                    monthEnd,
                    key.sessionDate()
            );

            if (impactedDates.isEmpty()) {
                expanded.add(key);
                continue;
            }

            for (LocalDate impactedDate : impactedDates) {
                expanded.add(new UserDateKey(key.userId(), impactedDate));
            }
        }
        return expanded;
    }

    private AggregatedDrivingMetrics loadMetrics(Long userId, LocalDate sessionDate) {
        LocalDate monthStart = sessionDate.withDayOfMonth(1);
        SessionMetrics sessionMetrics = jdbcTemplate.query("""
                        select
                            count(*) as session_count,
                            coalesce(sum(distance_km), 0) as total_distance_km,
                            coalesce(sum(driving_time_minutes), 0) as total_driving_time_minutes,
                            coalesce(sum(idling_time_minutes), 0) as total_idling_time_minutes,
                            max(user_vehicle_id) as user_vehicle_id
                        from driving_sessions
                        where user_id = ?
                          and session_date between ? and ?
                        """,
                rs -> rs.next()
                        ? new SessionMetrics(
                        rs.getInt("session_count"),
                        rs.getBigDecimal("total_distance_km"),
                        rs.getInt("total_driving_time_minutes"),
                        rs.getInt("total_idling_time_minutes"),
                        rs.getLong("user_vehicle_id")
                        )
                        : null,
                userId,
                monthStart,
                sessionDate
        );

        if (sessionMetrics == null || sessionMetrics.userVehicleId() == 0L) {
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
                monthStart,
                sessionDate
        );

        VehicleProfile vehicleProfile = loadVehicleProfile(sessionMetrics.userVehicleId());

        int sessionCount = Math.max(sessionMetrics.sessionCount(), 1);
        double rapidAccelPerSession = (double) eventMetrics.rapidAccelCount() / sessionCount;
        double hardBrakePerSession = (double) eventMetrics.hardBrakeCount() / sessionCount;
        double overspeedPerSession = (double) eventMetrics.overspeedCount() / sessionCount;

        int safetyPenalty = (int) Math.round(
                (rapidAccelPerSession * 4)
                        + (hardBrakePerSession * 5)
                        + (overspeedPerSession * 6)
        );
        int safetyScore = clamp(100 - Math.min(safetyPenalty, 45));

        double idleRatio = sessionMetrics.totalDrivingTimeMinutes() == 0
                ? 0
                : (double) sessionMetrics.totalIdlingTimeMinutes() / sessionMetrics.totalDrivingTimeMinutes();

        int ecoPenalty = (int) Math.round(
                (idleRatio * 50)
                        + (rapidAccelPerSession * 5)
                        + (hardBrakePerSession * 3)
        );
        int ecoScore = clamp(100 - Math.min(ecoPenalty, 45));

        BigDecimal actualCo2G = sessionMetrics.totalDistanceKm()
                .multiply(vehicleProfile.baseEmissionFactor())
                .multiply(vehicleProfile.vehicleSizeFactor())
                .add(BigDecimal.valueOf(sessionMetrics.totalIdlingTimeMinutes()).multiply(vehicleProfile.idleFactor()))
                .add(BigDecimal.valueOf(eventMetrics.rapidAccelCount()).multiply(vehicleProfile.accelFactor()))
                .add(BigDecimal.valueOf(eventMetrics.hardBrakeCount()).multiply(vehicleProfile.decelFactor()));

        BigDecimal baselineCo2G = sessionMetrics.totalDistanceKm()
                .multiply(BigDecimal.valueOf(160))
                .multiply(vehicleProfile.vehicleSizeFactor())
                .add(BigDecimal.valueOf(sessionMetrics.totalIdlingTimeMinutes()).multiply(BigDecimal.valueOf(25)))
                .add(BigDecimal.valueOf(eventMetrics.rapidAccelCount()).multiply(BigDecimal.valueOf(18)))
                .add(BigDecimal.valueOf(eventMetrics.hardBrakeCount()).multiply(BigDecimal.valueOf(12)));

        BigDecimal carbonReductionKg = baselineCo2G.subtract(actualCo2G).max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP);
        int rewardPoint = carbonReductionKg.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.DOWN)
                .intValue();

        return new AggregatedDrivingMetrics(safetyScore, ecoScore, carbonReductionKg, rewardPoint);
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

    private void upsertSafetySnapshot(Long userId, LocalDate sessionDate, int safetyScore) {
        Long snapshotId = jdbcTemplate.query("""
                        select id
                        from driving_score_snapshots
                        where user_id = ? and snapshot_date = ?
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId,
                sessionDate
        );

        if (snapshotId == null) {
            jdbcTemplate.update("""
                            insert into driving_score_snapshots (user_id, snapshot_date, score, created_at)
                            values (?, ?, ?, ?)
                            """,
                    userId,
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

    private void upsertCarbonSnapshot(Long userId, LocalDate sessionDate, BigDecimal carbonReductionKg, int rewardPoint) {
        Long snapshotId = jdbcTemplate.query("""
                        select id
                        from carbon_reduction_snapshots
                        where user_id = ? and snapshot_date = ?
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId,
                sessionDate
        );

        if (snapshotId == null) {
            jdbcTemplate.update("""
                            insert into carbon_reduction_snapshots (
                                user_id,
                                snapshot_date,
                                carbon_reduction_kg,
                                reward_point,
                                created_at
                            ) values (?, ?, ?, ?, ?)
                            """,
                    userId,
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

    private void upsertScoreChangeLog(Long userId, LocalDate sessionDate, int safetyScore) {
        Long snapshotId = jdbcTemplate.query("""
                        select id
                        from driving_score_snapshots
                        where user_id = ? and snapshot_date = ?
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId,
                sessionDate
        );

        if (snapshotId == null) {
            return;
        }

        Integer previousScore = jdbcTemplate.query("""
                        select score
                        from driving_score_snapshots
                        where user_id = ? and snapshot_date < ?
                          and date_trunc('month', snapshot_date) = date_trunc('month', ?::date)
                        order by snapshot_date desc
                        limit 1
                        """,
                rs -> rs.next() ? rs.getInt("score") : null,
                userId,
                sessionDate,
                Date.valueOf(sessionDate)
        );

        int scoreDelta = previousScore == null ? safetyScore - 100 : safetyScore - previousScore;

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
            int totalIdlingTimeMinutes,
            long userVehicleId
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
