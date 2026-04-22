package com.gorani.ecodrive.vehicle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserVehicleQueryService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<UserVehicleSummary> getMyVehicles(Long userId) {
        return jdbcTemplate.query(
                """
                select uv.id,
                       uv.vehicle_number,
                       uv.vehicle_model_id,
                       uv.status,
                       uv.registered_at,
                       vm.manufacturer,
                       vm.model_name,
                       vm.model_year,
                       vm.fuel_type
                from user_vehicles uv
                join vehicle_models vm on vm.id = uv.vehicle_model_id
                where uv.user_id = ?
                  and uv.status = 'ACTIVE'
                order by uv.registered_at desc, uv.id desc
                """,
                (rs, rowNum) -> new UserVehicleSummary(
                        rs.getLong("id"),
                        rs.getString("vehicle_number"),
                        rs.getLong("vehicle_model_id"),
                        rs.getString("manufacturer"),
                        rs.getString("model_name"),
                        rs.getShort("model_year"),
                        rs.getString("fuel_type"),
                        rs.getString("status"),
                        rs.getTimestamp("registered_at").toLocalDateTime()
                ),
                userId
        );
    }

    public record UserVehicleSummary(
            Long userVehicleId,
            String vehicleNumber,
            Long vehicleModelId,
            String manufacturer,
            String modelName,
            short modelYear,
            String fuelType,
            String status,
            LocalDateTime registeredAt
    ) {
    }
}
