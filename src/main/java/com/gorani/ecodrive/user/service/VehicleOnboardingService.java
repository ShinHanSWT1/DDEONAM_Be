package com.gorani.ecodrive.user.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleOnboardingService {

    private final JdbcTemplate jdbcTemplate;

    public Long registerVehicle(Long userId, Long requestedVehicleModelId, String vehicleNumber, LocalDateTime now) {
        Long vehicleModelId = findVehicleModelId(requestedVehicleModelId);
        return insertUserVehicle(userId, vehicleModelId, vehicleNumber, now);
    }

    private Long findVehicleModelId(Long requestedVehicleModelId) {
        Long vehicleModelId = jdbcTemplate.query(
                """
                select id
                from vehicle_models
                where id = ?
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                requestedVehicleModelId
        );

        if (vehicleModelId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return vehicleModelId;
    }

    private Long insertUserVehicle(Long userId, Long vehicleModelId, String vehicleNumber, LocalDateTime now) {
        return insertAndReturnId(
                """
                insert into user_vehicles
                (user_id, vehicle_model_id, vehicle_number, status, registered_at, updated_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                ps -> {
                    ps.setLong(1, userId);
                    ps.setLong(2, vehicleModelId);
                    ps.setString(3, vehicleNumber);
                    ps.setString(4, "ACTIVE");
                    ps.setTimestamp(5, Timestamp.valueOf(now));
                    ps.setTimestamp(6, Timestamp.valueOf(now));
                }
        );
    }

    private Long insertAndReturnId(String sql, SqlBinder binder) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            binder.bind(ps);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return key.longValue();
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement preparedStatement) throws java.sql.SQLException;
    }
}
