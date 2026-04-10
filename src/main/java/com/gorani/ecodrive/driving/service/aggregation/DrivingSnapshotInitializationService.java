package com.gorani.ecodrive.driving.service.aggregation;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DrivingSnapshotInitializationService {

    private static final int DEFAULT_SAFETY_SCORE = 100;
    private static final BigDecimal DEFAULT_CARBON_REDUCTION_KG = BigDecimal.ZERO;
    private static final int DEFAULT_REWARD_POINT = 0;

    private final JdbcTemplate jdbcTemplate;

    public void initializeDefaults(Long userId, LocalDate snapshotDate, LocalDateTime createdAt) {
        initializeSafetySnapshot(userId, snapshotDate, createdAt);
        initializeCarbonSnapshot(userId, snapshotDate, createdAt);
    }

    private void initializeSafetySnapshot(Long userId, LocalDate snapshotDate, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                        insert into driving_score_snapshots (user_id, snapshot_date, score, created_at)
                        select ?, ?, ?, ?
                        where not exists (
                            select 1
                            from driving_score_snapshots
                            where user_id = ?
                              and snapshot_date = ?
                        )
                        """,
                userId,
                Date.valueOf(snapshotDate),
                DEFAULT_SAFETY_SCORE,
                Timestamp.valueOf(createdAt),
                userId,
                Date.valueOf(snapshotDate)
        );
    }

    private void initializeCarbonSnapshot(Long userId, LocalDate snapshotDate, LocalDateTime createdAt) {
        jdbcTemplate.update("""
                        insert into carbon_reduction_snapshots (
                            user_id,
                            snapshot_date,
                            carbon_reduction_kg,
                            reward_point,
                            created_at
                        )
                        select ?, ?, ?, ?, ?
                        where not exists (
                            select 1
                            from carbon_reduction_snapshots
                            where user_id = ?
                              and snapshot_date = ?
                        )
                        """,
                userId,
                Date.valueOf(snapshotDate),
                DEFAULT_CARBON_REDUCTION_KG,
                DEFAULT_REWARD_POINT,
                Timestamp.valueOf(createdAt),
                userId,
                Date.valueOf(snapshotDate)
        );
    }
}
