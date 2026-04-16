package com.gorani.ecodrive.driving.service.query;

import com.gorani.ecodrive.driving.dto.query.DrivingBehaviorSummaryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingDailySummaryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingLatestCarbonResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingLatestScoreResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingMonthlySummaryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingRecentSessionResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingScoreHistoryResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingScoreTrendResponse;
import com.gorani.ecodrive.driving.dto.query.DrivingWeeklySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrivingQueryService {

    private static final BigDecimal ZERO_DECIMAL = BigDecimal.ZERO;

    private final JdbcTemplate jdbcTemplate;

    private String optionalVehicleFilter(String columnName) {
        return " and (cast(? as bigint) is null or " + columnName + " = cast(? as bigint))";
    }

    public DrivingLatestScoreResponse getLatestScore(Long userId) {
        return getLatestScore(userId, null);
    }

    public DrivingLatestScoreResponse getLatestScore(Long userId, Long userVehicleId) {
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        return jdbcTemplate.query("""
                        select snapshot_date, score
                        from driving_score_snapshots
                        where user_id = ?
                        """.concat(optionalVehicleFilter("user_vehicle_id")).concat("""
                          and snapshot_date >= ?
                        order by snapshot_date desc, id desc
                        limit 1
                        """),
                rs -> rs.next() ? mapLatestScore(rs) : new DrivingLatestScoreResponse(null, null),
                userId,
                userVehicleId,
                userVehicleId,
                currentMonthStart
        );
    }

    public List<DrivingRecentSessionResponse> getRecentSessions(Long userId, int limit) {
        return getRecentSessions(userId, null, limit);
    }

    public List<DrivingRecentSessionResponse> getRecentSessions(Long userId, Long userVehicleId, int limit) {
        return jdbcTemplate.query("""
                        select
                            id,
                            external_key,
                            session_date,
                            started_at,
                            ended_at,
                            distance_km,
                            driving_time_minutes,
                            idling_time_minutes,
                            average_speed,
                            max_speed
                        from driving_sessions
                        where user_id = ?
                        """.concat(optionalVehicleFilter("user_vehicle_id")).concat("""
                        order by started_at desc, id desc
                        limit ?
                        """),
                recentSessionRowMapper(),
                userId,
                userVehicleId,
                userVehicleId,
                limit
        );
    }

    public DrivingLatestCarbonResponse getLatestCarbon(Long userId) {
        return getLatestCarbon(userId, null);
    }

    public DrivingLatestCarbonResponse getLatestCarbon(Long userId, Long userVehicleId) {
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        return jdbcTemplate.query("""
                        select
                            snapshot_date,
                            carbon_reduction_kg,
                            reward_point
                        from carbon_reduction_snapshots
                        where user_id = ?
                        """.concat(optionalVehicleFilter("user_vehicle_id")).concat("""
                          and snapshot_date >= ?
                        order by snapshot_date desc, id desc
                        limit 1
                        """),
                rs -> rs.next() ? mapLatestCarbon(rs) : new DrivingLatestCarbonResponse(null, null, null),
                userId,
                userVehicleId,
                userVehicleId,
                currentMonthStart
        );
    }

    public DrivingDailySummaryResponse getDailySummary(Long userId, LocalDate date) {
        return getDailySummary(userId, null, date);
    }

    public DrivingDailySummaryResponse getDailySummary(Long userId, Long userVehicleId, LocalDate date) {
        return jdbcTemplate.query("""
                        select
                            s.session_date,
                            count(*) as session_count,
                            coalesce(sum(s.distance_km), 0) as total_distance_km,
                            coalesce(sum(s.driving_time_minutes), 0) as total_driving_time_minutes,
                            coalesce(sum(s.idling_time_minutes), 0) as total_idling_time_minutes,
                            coalesce(avg(s.average_speed), 0) as average_speed,
                            coalesce(max(s.max_speed), 0) as max_speed,
                            min(s.started_at) as first_started_at,
                            max(s.ended_at) as last_ended_at,
                            coalesce(sum(case when e.event_type = 'RAPID_ACCEL' then 1 else 0 end), 0) as rapid_accel_count,
                            coalesce(sum(case when e.event_type = 'HARD_BRAKE' then 1 else 0 end), 0) as hard_brake_count,
                            coalesce(sum(case when e.event_type = 'OVERSPEED' then 1 else 0 end), 0) as overspeed_count
                        from driving_sessions s
                        left join driving_events e on e.driving_session_id = s.id
                        where s.user_id = ?
                        """.concat(optionalVehicleFilter("s.user_vehicle_id")).concat("""
                          and s.session_date = ?
                        group by s.session_date
                        """),
                rs -> rs.next() ? mapDailySummary(rs) : emptyDailySummary(date),
                userId,
                userVehicleId,
                userVehicleId,
                date
        );
    }

    public DrivingBehaviorSummaryResponse getBehaviorSummary(Long userId, LocalDate date) {
        return getBehaviorSummary(userId, null, date);
    }

    public DrivingBehaviorSummaryResponse getBehaviorSummary(Long userId, Long userVehicleId, LocalDate date) {
        return jdbcTemplate.query("""
                        select
                            ? as session_date,
                            coalesce(sum(case when e.event_type = 'RAPID_ACCEL' then 1 else 0 end), 0) as rapid_accel_count,
                            coalesce(sum(case when e.event_type = 'HARD_BRAKE' then 1 else 0 end), 0) as hard_brake_count,
                            coalesce(sum(case when e.event_type = 'OVERSPEED' then 1 else 0 end), 0) as overspeed_count,
                            coalesce(sum(case
                                when extract(hour from s.started_at) >= 22
                                  or extract(hour from s.started_at) < 6
                                  or extract(hour from s.ended_at) >= 22
                                  or extract(hour from s.ended_at) < 6
                                then 1 else 0 end), 0) as night_driving_count,
                            coalesce(sum(s.idling_time_minutes), 0) as total_idling_time_minutes
                        from driving_sessions s
                        left join driving_events e on e.driving_session_id = s.id
                        where s.user_id = ?
                        """.concat(optionalVehicleFilter("s.user_vehicle_id")).concat("""
                          and s.session_date = ?
                        """),
                rs -> rs.next() ? mapBehaviorSummary(rs) : emptyBehaviorSummary(date),
                date,
                userId,
                userVehicleId,
                userVehicleId,
                date
        );
    }

    public List<DrivingWeeklySummaryResponse> getWeeklySummaries(Long userId, int year, int month) {
        return getWeeklySummaries(userId, null, year, month);
    }

    public List<DrivingWeeklySummaryResponse> getWeeklySummaries(Long userId, Long userVehicleId, int year, int month) {
        return jdbcTemplate.query("""
                        with daily_metrics as (
                            select
                                s.session_date,
                                ((extract(day from s.session_date)::int - 1) / 7) + 1 as week_of_month,
                                count(*) as session_count,
                                coalesce(sum(s.distance_km), 0) as total_distance_km,
                                coalesce(sum(s.idling_time_minutes), 0) as total_idling_time_minutes,
                                coalesce(avg(s.average_speed), 0) as average_speed,
                                coalesce(max(s.max_speed), 0) as max_speed
                            from driving_sessions s
                            where s.user_id = ?
                        """.concat(optionalVehicleFilter("s.user_vehicle_id")).concat("""
                              and extract(year from s.session_date) = ?
                              and extract(month from s.session_date) = ?
                            group by s.session_date
                        )
                        select
                            ? as year,
                            ? as month,
                            week_of_month,
                            min(session_date) as start_date,
                            max(session_date) as end_date,
                            count(*) as day_count,
                            coalesce(sum(session_count), 0) as session_count,
                            coalesce(avg(total_distance_km), 0) as average_distance_km,
                            coalesce(avg(total_idling_time_minutes), 0) as average_idling_time_minutes,
                            coalesce(avg(average_speed), 0) as average_speed,
                            coalesce(max(max_speed), 0) as max_speed
                        from daily_metrics
                        group by week_of_month
                        order by week_of_month
                        """),
                weeklySummaryRowMapper(),
                userId,
                userVehicleId,
                userVehicleId,
                year,
                month,
                year,
                month
        );
    }

    public DrivingMonthlySummaryResponse getMonthlySummary(Long userId, int year, int month) {
        return getMonthlySummary(userId, null, year, month);
    }

    public DrivingMonthlySummaryResponse getMonthlySummary(Long userId, Long userVehicleId, int year, int month) {
        String sql = """
                        with session_metrics as (
                            select
                                count(*) as session_count,
                                count(distinct session_date) as day_count,
                                coalesce(sum(distance_km), 0) as total_distance_km,
                                coalesce(sum(driving_time_minutes), 0) as total_driving_time_minutes,
                                coalesce(sum(idling_time_minutes), 0) as total_idling_time_minutes,
                                coalesce(avg(average_speed), 0) as average_speed,
                                coalesce(max(max_speed), 0) as max_speed
                            from driving_sessions
                            where user_id = ?
                        """
                + optionalVehicleFilter("user_vehicle_id")
                + """
                              and extract(year from session_date) = ?
                              and extract(month from session_date) = ?
                        ),
                        event_metrics as (
                            select
                                coalesce(sum(case when e.event_type = 'RAPID_ACCEL' then 1 else 0 end), 0) as rapid_accel_count,
                                coalesce(sum(case when e.event_type = 'HARD_BRAKE' then 1 else 0 end), 0) as hard_brake_count,
                                coalesce(sum(case when e.event_type = 'OVERSPEED' then 1 else 0 end), 0) as overspeed_count
                            from driving_events e
                            join driving_sessions s on e.driving_session_id = s.id
                            where e.user_id = ?
                        """
                + optionalVehicleFilter("s.user_vehicle_id")
                + """
                              and extract(year from s.session_date) = ?
                              and extract(month from s.session_date) = ?
                        ),
                        latest_carbon as (
                            select
                                carbon_reduction_kg,
                                reward_point
                            from carbon_reduction_snapshots
                            where user_id = ?
                        """
                + optionalVehicleFilter("user_vehicle_id")
                + """
                              and extract(year from snapshot_date) = ?
                              and extract(month from snapshot_date) = ?
                            order by snapshot_date desc, id desc
                            limit 1
                        ),
                        carbon_metrics as (
                            select
                                coalesce((select carbon_reduction_kg from latest_carbon), 0) as carbon_reduction_kg,
                                coalesce((select reward_point from latest_carbon), 0) as reward_point
                        )
                        select
                            ? as year,
                            ? as month,
                            sm.session_count,
                            sm.day_count,
                            sm.total_distance_km,
                            sm.total_driving_time_minutes,
                            sm.total_idling_time_minutes,
                            sm.average_speed,
                            sm.max_speed,
                            em.rapid_accel_count,
                            em.hard_brake_count,
                            em.overspeed_count,
                            case
                                when sm.total_driving_time_minutes = 0 then 0
                                else round(((sm.total_driving_time_minutes - sm.total_idling_time_minutes)::numeric / sm.total_driving_time_minutes) * 100, 2)
                            end as steady_driving_ratio,
                            cm.carbon_reduction_kg,
                            cm.reward_point
                        from session_metrics sm
                        cross join event_metrics em
                        cross join carbon_metrics cm
                        """;
        return jdbcTemplate.query(
                sql,
                rs -> rs.next() && rs.getInt("session_count") > 0
                        ? mapMonthlySummary(rs)
                        : emptyMonthlySummary(year, month),
                userId,
                userVehicleId,
                userVehicleId,
                year,
                month,
                userId,
                userVehicleId,
                userVehicleId,
                year,
                month,
                userId,
                userVehicleId,
                userVehicleId,
                year,
                month,
                year,
                month
        );
    }

    public List<DrivingScoreTrendResponse> getScoreTrend(Long userId, int year, int month) {
        return getScoreTrend(userId, null, year, month);
    }

    public List<DrivingScoreTrendResponse> getScoreTrend(Long userId, Long userVehicleId, int year, int month) {
        return jdbcTemplate.query("""
                        select snapshot_date, score
                        from driving_score_snapshots
                        where user_id = ?
                        """.concat(optionalVehicleFilter("user_vehicle_id")).concat("""
                          and extract(year from snapshot_date) = ?
                          and extract(month from snapshot_date) = ?
                        order by snapshot_date asc, id asc
                        """),
                scoreTrendRowMapper(),
                userId,
                userVehicleId,
                userVehicleId,
                year,
                month
        );
    }

    public List<DrivingScoreHistoryResponse> getScoreHistory(Long userId, int limit) {
        return getScoreHistory(userId, null, limit);
    }

    public List<DrivingScoreHistoryResponse> getScoreHistory(Long userId, Long userVehicleId, int limit) {
        return jdbcTemplate.query("""
                        select dcl.id, dcl.change_type, dcl.message, dcl.score_delta, dcl.change_date
                        from driving_score_change_logs dcl
                        join driving_score_snapshots dss on dss.id = dcl.snapshot_id
                        where dcl.user_id = ?
                        """.concat(optionalVehicleFilter("dss.user_vehicle_id")).concat("""
                        order by dcl.change_date desc, dcl.display_order asc, dcl.id desc
                        limit ?
                        """),
                scoreHistoryRowMapper(),
                userId,
                userVehicleId,
                userVehicleId,
                limit
        );
    }

    private DrivingLatestScoreResponse mapLatestScore(ResultSet rs) throws SQLException {
        return new DrivingLatestScoreResponse(
                rs.getObject("snapshot_date", LocalDate.class),
                rs.getInt("score")
        );
    }

    private DrivingLatestCarbonResponse mapLatestCarbon(ResultSet rs) throws SQLException {
        return new DrivingLatestCarbonResponse(
                rs.getObject("snapshot_date", LocalDate.class),
                rs.getBigDecimal("carbon_reduction_kg"),
                rs.getInt("reward_point")
        );
    }

    private DrivingDailySummaryResponse mapDailySummary(ResultSet rs) throws SQLException {
        return new DrivingDailySummaryResponse(
                rs.getObject("session_date", LocalDate.class),
                rs.getInt("session_count"),
                rs.getBigDecimal("total_distance_km"),
                rs.getInt("total_driving_time_minutes"),
                rs.getInt("total_idling_time_minutes"),
                rs.getBigDecimal("average_speed"),
                rs.getBigDecimal("max_speed"),
                rs.getInt("rapid_accel_count"),
                rs.getInt("hard_brake_count"),
                rs.getInt("overspeed_count"),
                rs.getTimestamp("first_started_at").toLocalDateTime(),
                rs.getTimestamp("last_ended_at").toLocalDateTime()
        );
    }

    private DrivingDailySummaryResponse emptyDailySummary(LocalDate date) {
        return new DrivingDailySummaryResponse(date, 0, null, null, null, null, null, null, null, null, null, null);
    }

    private DrivingBehaviorSummaryResponse mapBehaviorSummary(ResultSet rs) throws SQLException {
        return new DrivingBehaviorSummaryResponse(
                rs.getObject("session_date", LocalDate.class),
                rs.getInt("rapid_accel_count"),
                rs.getInt("hard_brake_count"),
                rs.getInt("overspeed_count"),
                rs.getInt("night_driving_count"),
                rs.getInt("total_idling_time_minutes")
        );
    }

    private DrivingBehaviorSummaryResponse emptyBehaviorSummary(LocalDate date) {
        return new DrivingBehaviorSummaryResponse(date, 0, 0, 0, 0, 0);
    }

    private DrivingMonthlySummaryResponse mapMonthlySummary(ResultSet rs) throws SQLException {
        return new DrivingMonthlySummaryResponse(
                rs.getInt("year"),
                rs.getInt("month"),
                rs.getInt("session_count"),
                rs.getInt("day_count"),
                rs.getBigDecimal("total_distance_km"),
                rs.getInt("total_driving_time_minutes"),
                rs.getInt("total_idling_time_minutes"),
                rs.getBigDecimal("average_speed"),
                rs.getBigDecimal("max_speed"),
                rs.getInt("rapid_accel_count"),
                rs.getInt("hard_brake_count"),
                rs.getInt("overspeed_count"),
                rs.getBigDecimal("steady_driving_ratio"),
                rs.getBigDecimal("carbon_reduction_kg"),
                rs.getInt("reward_point")
        );
    }

    private DrivingMonthlySummaryResponse emptyMonthlySummary(int year, int month) {
        return new DrivingMonthlySummaryResponse(
                year,
                month,
                0,
                0,
                ZERO_DECIMAL,
                0,
                0,
                null,
                null,
                0,
                0,
                0,
                ZERO_DECIMAL,
                ZERO_DECIMAL,
                0
        );
    }

    private RowMapper<DrivingRecentSessionResponse> recentSessionRowMapper() {
        return (rs, rowNum) -> new DrivingRecentSessionResponse(
                rs.getLong("id"),
                rs.getString("external_key"),
                rs.getObject("session_date", LocalDate.class),
                rs.getTimestamp("started_at").toLocalDateTime(),
                rs.getTimestamp("ended_at").toLocalDateTime(),
                rs.getBigDecimal("distance_km"),
                rs.getInt("driving_time_minutes"),
                rs.getInt("idling_time_minutes"),
                rs.getBigDecimal("average_speed"),
                rs.getBigDecimal("max_speed")
        );
    }

    private RowMapper<DrivingWeeklySummaryResponse> weeklySummaryRowMapper() {
        return (rs, rowNum) -> {
            int weekOfMonth = rs.getInt("week_of_month");
            int month = rs.getInt("month");
            return new DrivingWeeklySummaryResponse(
                    rs.getInt("year"),
                    month,
                    weekOfMonth,
                    month + "월 " + weekOfMonth + "주차",
                    rs.getObject("start_date", LocalDate.class),
                    rs.getObject("end_date", LocalDate.class),
                    rs.getInt("day_count"),
                    rs.getInt("session_count"),
                    rs.getBigDecimal("average_distance_km"),
                    rs.getBigDecimal("average_idling_time_minutes"),
                    rs.getBigDecimal("average_speed"),
                    rs.getBigDecimal("max_speed")
            );
        };
    }

    private RowMapper<DrivingScoreTrendResponse> scoreTrendRowMapper() {
        return (rs, rowNum) -> new DrivingScoreTrendResponse(
                rs.getObject("snapshot_date", LocalDate.class),
                rs.getInt("score")
        );
    }

    private RowMapper<DrivingScoreHistoryResponse> scoreHistoryRowMapper() {
        return (rs, rowNum) -> new DrivingScoreHistoryResponse(
                rs.getLong("id"),
                rs.getString("change_type"),
                rs.getString("message"),
                rs.getInt("score_delta"),
                rs.getObject("change_date", LocalDate.class)
        );
    }
}
