package com.gorani.ecodrive.driving.service.insight;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DrivingInsightQueryService {

    private static final String KST_TODAY_SQL = "(now() at time zone 'Asia/Seoul')::date";
    private final JdbcTemplate jdbcTemplate;

    public DrivingInsightFeatures loadFeatures(Long userId, Long userVehicleId) {
        return jdbcTemplate.query(("""
                        with session_metrics as (
                            select
                                count(*) as session_count,
                                coalesce(sum(s.distance_km), 0) as total_distance_km,
                                coalesce(sum(s.driving_time_minutes), 0) as total_driving_minutes,
                                coalesce(sum(s.idling_time_minutes), 0) as total_idling_minutes,
                                coalesce(avg(s.average_speed), 0) as average_speed,
                                coalesce(max(s.max_speed), 0) as max_speed,
                                coalesce(sum(case
                                    when extract(hour from s.started_at) >= 22
                                      or extract(hour from s.started_at) < 6
                                      or extract(hour from s.ended_at) >= 22
                                      or extract(hour from s.ended_at) < 6
                                    then 1 else 0 end), 0) as night_session_count
                            from driving_sessions s
                            where s.user_id = ?
                              and s.session_date between %s - interval '6 day' and %s
                              and (cast(? as bigint) is null or s.user_vehicle_id = cast(? as bigint))
                        ),
                        event_metrics as (
                            select
                                coalesce(sum(case when e.event_type = 'RAPID_ACCEL' then 1 else 0 end), 0) as rapid_accel_count,
                                coalesce(sum(case when e.event_type = 'HARD_BRAKE' then 1 else 0 end), 0) as hard_brake_count,
                                coalesce(sum(case when e.event_type = 'OVERSPEED' then 1 else 0 end), 0) as overspeed_count
                            from driving_events e
                            join driving_sessions s on s.id = e.driving_session_id
                            where e.user_id = ?
                              and s.session_date between %s - interval '6 day' and %s
                              and (cast(? as bigint) is null or s.user_vehicle_id = cast(? as bigint))
                        )
                        select
                            sm.session_count,
                            sm.total_distance_km,
                            sm.total_driving_minutes,
                            sm.total_idling_minutes,
                            sm.average_speed,
                            sm.max_speed,
                            sm.night_session_count,
                            em.rapid_accel_count,
                            em.hard_brake_count,
                            em.overspeed_count
                        from session_metrics sm
                        cross join event_metrics em
                        """).formatted(KST_TODAY_SQL, KST_TODAY_SQL, KST_TODAY_SQL, KST_TODAY_SQL),
                rs -> {
                    if (!rs.next()) {
                        return empty();
                    }

                    int sessionCount = rs.getInt("session_count");
                    BigDecimal distanceKm = rs.getBigDecimal("total_distance_km");
                    int drivingMinutes = rs.getInt("total_driving_minutes");
                    int idlingMinutes = rs.getInt("total_idling_minutes");
                    int nightSessionCount = rs.getInt("night_session_count");
                    int rapidAccelCount = rs.getInt("rapid_accel_count");
                    int hardBrakeCount = rs.getInt("hard_brake_count");
                    int overspeedCount = rs.getInt("overspeed_count");

                    double distance = distanceKm == null ? 0.0 : distanceKm.doubleValue();
                    double overspeedEventRatio = sessionCount == 0 ? 0.0 : clampRatio((double) overspeedCount / sessionCount);
                    double idlingRatio = drivingMinutes == 0 ? 0.0 : clampRatio((double) idlingMinutes / drivingMinutes);
                    double nightDrivingRatio = sessionCount == 0 ? 0.0 : clampRatio((double) nightSessionCount / sessionCount);
                    double hardAccelPer100km = distance <= 0 ? 0.0 : round((rapidAccelCount * 100.0) / distance);
                    double hardBrakePer100km = distance <= 0 ? 0.0 : round((hardBrakeCount * 100.0) / distance);

                    return new DrivingInsightFeatures(
                            sessionCount,
                            distanceKm == null ? BigDecimal.ZERO : distanceKm.setScale(2, RoundingMode.HALF_UP),
                            drivingMinutes,
                            rs.getDouble("average_speed"),
                            rs.getDouble("max_speed"),
                            hardAccelPer100km,
                            hardBrakePer100km,
                            round(overspeedEventRatio),
                            round(idlingRatio),
                            round(nightDrivingRatio)
                    );
                },
                userId,
                userVehicleId,
                userVehicleId,
                userId,
                userVehicleId,
                userVehicleId
        );
    }

    private DrivingInsightFeatures empty() {
        return new DrivingInsightFeatures(0, BigDecimal.ZERO, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double clampRatio(double value) {
        return Math.max(0.0, Math.min(value, 1.0));
    }
}
