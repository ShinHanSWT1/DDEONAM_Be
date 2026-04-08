package com.gorani.ecodrive.driving.service;

import com.gorani.ecodrive.driving.dto.DrivingLatestCarbonResponse;
import com.gorani.ecodrive.driving.dto.DrivingLatestScoreResponse;
import com.gorani.ecodrive.driving.dto.DrivingRecentSessionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DrivingQueryService {

    private final JdbcTemplate jdbcTemplate;

    public DrivingLatestScoreResponse getLatestScore(Long userId) {
        return jdbcTemplate.query("""
                        select snapshot_date, score
                        from driving_score_snapshots
                        where user_id = ?
                        order by snapshot_date desc, id desc
                        limit 1
                        """,
                rs -> rs.next()
                        ? new DrivingLatestScoreResponse(
                        rs.getObject("snapshot_date", java.time.LocalDate.class),
                        rs.getInt("score")
                )
                        : new DrivingLatestScoreResponse(null, null),
                userId
        );
    }

    public List<DrivingRecentSessionResponse> getRecentSessions(Long userId, int limit) {
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
                        order by started_at desc, id desc
                        limit ?
                        """,
                (rs, rowNum) -> new DrivingRecentSessionResponse(
                        rs.getLong("id"),
                        rs.getString("external_key"),
                        rs.getObject("session_date", java.time.LocalDate.class),
                        rs.getTimestamp("started_at").toLocalDateTime(),
                        rs.getTimestamp("ended_at").toLocalDateTime(),
                        rs.getBigDecimal("distance_km"),
                        rs.getInt("driving_time_minutes"),
                        rs.getInt("idling_time_minutes"),
                        rs.getBigDecimal("average_speed"),
                        rs.getBigDecimal("max_speed")
                ),
                userId,
                limit
        );
    }

    public DrivingLatestCarbonResponse getLatestCarbon(Long userId) {
        return jdbcTemplate.query("""
                        select snapshot_date, carbon_reduction_kg, reward_point
                        from carbon_reduction_snapshots
                        where user_id = ?
                        order by snapshot_date desc, id desc
                        limit 1
                        """,
                rs -> rs.next()
                        ? new DrivingLatestCarbonResponse(
                        rs.getObject("snapshot_date", java.time.LocalDate.class),
                        rs.getBigDecimal("carbon_reduction_kg"),
                        rs.getInt("reward_point")
                )
                        : new DrivingLatestCarbonResponse(null, null, null),
                userId
        );
    }
}
