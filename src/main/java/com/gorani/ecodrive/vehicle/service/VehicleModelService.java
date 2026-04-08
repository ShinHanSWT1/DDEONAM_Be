package com.gorani.ecodrive.vehicle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleModelService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<VehicleModelSummary> searchModels(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String likeKeyword = "%" + normalizedKeyword.toLowerCase() + "%";

        return jdbcTemplate.query(
                """
                select id, manufacturer, model_name, model_year, fuel_type
                from vehicle_models
                where ? = ''
                   or lower(manufacturer) like ?
                   or lower(model_name) like ?
                   or cast(model_year as text) like ?
                order by manufacturer asc, model_name asc, model_year desc
                limit 20
                """,
                (rs, rowNum) -> new VehicleModelSummary(
                        rs.getLong("id"),
                        rs.getString("manufacturer"),
                        rs.getString("model_name"),
                        rs.getShort("model_year"),
                        rs.getString("fuel_type")
                ),
                normalizedKeyword,
                likeKeyword,
                likeKeyword,
                likeKeyword
        );
    }

    public record VehicleModelSummary(
            Long id,
            String manufacturer,
            String modelName,
            short modelYear,
            String fuelType
    ) {
    }
}
