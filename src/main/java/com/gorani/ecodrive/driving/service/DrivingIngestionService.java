package com.gorani.ecodrive.driving.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.driving.dto.DummyDrivingBatch;
import com.gorani.ecodrive.driving.dto.DummyDrivingEventPayload;
import com.gorani.ecodrive.driving.dto.DummyDrivingSessionPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DrivingIngestionService {

    private final JdbcTemplate jdbcTemplate;

    public IngestionSummary ingest(DummyDrivingBatch batch) {
        if (batch.sessions() == null || batch.sessions().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        int insertedSessions = 0;
        int insertedEvents = 0;
        Set<UserDateKey> affectedUserDates = new LinkedHashSet<>();

        for (DummyDrivingSessionPayload session : batch.sessions()) {
            validate(session);

            if (existsByExternalKey(session.externalKey())) {
                continue;
            }

            Long sessionId = insertSession(session);
            insertedSessions++;
            affectedUserDates.add(new UserDateKey(session.userId(), session.sessionDate()));

            List<DummyDrivingEventPayload> events = session.events() == null ? List.of() : session.events();
            for (DummyDrivingEventPayload event : events) {
                insertEvent(sessionId, session.userId(), event);
                insertedEvents++;
            }
        }

        return new IngestionSummary(insertedSessions, insertedEvents, new ArrayList<>(affectedUserDates));
    }

    private boolean existsByExternalKey(String externalKey) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from driving_sessions where external_key = ?",
                Integer.class,
                externalKey
        );
        return count != null && count > 0;
    }

    private Long insertSession(DummyDrivingSessionPayload session) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into driving_sessions (
                        user_id,
                        user_vehicle_id,
                        session_date,
                        started_at,
                        ended_at,
                        distance_km,
                        driving_time_minutes,
                        idling_time_minutes,
                        average_speed,
                        max_speed,
                        created_at,
                        external_key
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            ps.setLong(1, session.userId());
            ps.setLong(2, session.userVehicleId());
            ps.setObject(3, session.sessionDate());
            ps.setTimestamp(4, Timestamp.valueOf(session.startedAt()));
            ps.setTimestamp(5, Timestamp.valueOf(session.endedAt()));
            ps.setBigDecimal(6, session.distanceKm());
            ps.setInt(7, session.drivingTimeMinutes());
            ps.setInt(8, Objects.requireNonNullElse(session.idlingTimeMinutes(), 0));
            ps.setBigDecimal(9, session.averageSpeed());
            ps.setBigDecimal(10, session.maxSpeed());
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setString(12, session.externalKey());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return key.longValue();
    }

    private void insertEvent(Long sessionId, Long userId, DummyDrivingEventPayload event) {
        jdbcTemplate.update("""
                        insert into driving_events (
                            driving_session_id,
                            user_id,
                            event_type,
                            event_value,
                            occurred_at,
                            score_delta,
                            created_at
                        ) values (?, ?, ?, ?, ?, ?, ?)
                        """,
                sessionId,
                userId,
                event.eventType(),
                event.eventValue(),
                Timestamp.valueOf(event.occurredAt()),
                event.scoreDelta(),
                Timestamp.valueOf(LocalDateTime.now())
        );
    }

    private void validate(DummyDrivingSessionPayload session) {
        if (session.externalKey() == null
                || session.userId() == null
                || session.userVehicleId() == null
                || session.sessionDate() == null
                || session.startedAt() == null
                || session.endedAt() == null
                || session.distanceKm() == null
                || session.drivingTimeMinutes() == null
                || session.averageSpeed() == null
                || session.maxSpeed() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public record IngestionSummary(
            int insertedSessions,
            int insertedEvents,
            List<UserDateKey> affectedUserDates
    ) {
    }

    public record UserDateKey(Long userId, LocalDate sessionDate) {
    }
}
