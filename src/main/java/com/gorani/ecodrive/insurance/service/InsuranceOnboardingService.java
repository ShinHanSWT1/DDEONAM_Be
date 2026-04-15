package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class InsuranceOnboardingService {

    private static final String DEFAULT_INSURANCE_PRODUCT_NAME = "온보딩 표준형";
    private static final String DEFAULT_CONTRACT_STATUS = "ACTIVE";
    private static final String DEFAULT_CONTRACT_PLAN_TYPE = "STANDARD";

    private final JdbcTemplate jdbcTemplate;

    public InsuranceRegistrationIds registerInsurance(
            Long userId,
            Long requestedUserVehicleId,
            String insuranceCompanyName,
            String insuranceProductName,
            String planType,
            Integer annualPremium,
            LocalDate insuranceStartedAt,
            LocalDateTime now
    ) {
        Long userVehicleId = resolveUserVehicleId(userId, requestedUserVehicleId);
        Long insuranceCompanyId = findOrCreateInsuranceCompany(insuranceCompanyName, now);
        Long insuranceProductId = findOrCreateInsuranceProduct(
                insuranceCompanyId,
                insuranceProductName,
                annualPremium,
                now
        );
        // 실제 주행 점수 스냅샷이 있으면 연결, 없으면 null (주행 기록 없는 신규 사용자)
        Long drivingScoreSnapshotId = findExistingDrivingScoreSnapshot(userId, userVehicleId);
        
        // 여기서 planType을 정확히 전달해야 합니다.
        Long insuranceContractId = insertInsuranceContract(
                userId,
                insuranceProductId,
                drivingScoreSnapshotId,
                planType,
                insuranceStartedAt,
                annualPremium,
                now
        );
        
        Long userInsuranceId = insertUserInsurance(
                userId,
                userVehicleId,
                insuranceCompanyId,
                insuranceProductId,
                insuranceContractId,
                now
        );

        return new InsuranceRegistrationIds(
                userInsuranceId,
                userVehicleId,
                insuranceCompanyId,
                insuranceContractId
        );
    }

    private Long resolveUserVehicleId(Long userId, Long requestedUserVehicleId) {
        if (requestedUserVehicleId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Long existing = jdbcTemplate.query(
                """
                select id
                from user_vehicles
                where id = ?
                  and user_id = ?
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                requestedUserVehicleId,
                userId
        );

        if (existing == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Long activeUserInsuranceId = jdbcTemplate.query(
                """
                select id
                from user_insurances
                where user_id = ?
                  and user_vehicle_id = ?
                  and status = 'ACTIVE'
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId,
                requestedUserVehicleId
        );

        if (activeUserInsuranceId != null) {
            jdbcTemplate.update(
                    """
                    update user_insurances
                    set status = 'INACTIVE',
                        ended_at = ?
                    where id = ?
                    """,
                    Timestamp.valueOf(LocalDateTime.now()),
                    activeUserInsuranceId
            );
        }

        return existing;
    }

    private Long findOrCreateInsuranceCompany(String insuranceCompanyName, LocalDateTime now) {
        String normalizedCompanyName = insuranceCompanyName.trim();

        Long existingId = jdbcTemplate.query(
                """
                select id
                from insurance_companies
                where company_name = ?
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                normalizedCompanyName
        );

        if (existingId != null) {
            return existingId;
        }

        String companyCode = normalizedCompanyName
                .replaceAll("\\s+", "_")
                .toUpperCase(Locale.ROOT);

        return insertAndReturnId(
                """
                insert into insurance_companies
                (company_name, code, status, created_at)
                values (?, ?, ?, ?)
                """,
                ps -> {
                    ps.setString(1, normalizedCompanyName);
                    ps.setString(2, companyCode);
                    ps.setString(3, "ACTIVE");
                    ps.setTimestamp(4, Timestamp.valueOf(now));
                }
        );
    }

    private Long findOrCreateInsuranceProduct(
            Long insuranceCompanyId,
            String insuranceProductName,
            Integer annualPremium,
            LocalDateTime now
    ) {
        String normalizedProductName =
                insuranceProductName == null || insuranceProductName.isBlank()
                        ? DEFAULT_INSURANCE_PRODUCT_NAME
                        : insuranceProductName.trim();

        Long existingId = jdbcTemplate.query(
                """
                select id
                from insurance_products
                where insurance_company_id = ?
                  and product_name = ?
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                insuranceCompanyId,
                normalizedProductName
        );

        if (existingId != null) {
            return existingId;
        }

        return insertAndReturnId(
                """
                insert into insurance_products
                (insurance_company_id, product_name, base_amount, discount_rate, status, created_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                ps -> {
                    ps.setLong(1, insuranceCompanyId);
                    ps.setString(2, normalizedProductName);
                    ps.setInt(3, annualPremium);
                    ps.setBigDecimal(4, BigDecimal.ZERO);
                    ps.setString(5, "ACTIVE");
                    ps.setTimestamp(6, Timestamp.valueOf(now));
                }
        );
    }

    // 실제 주행 점수 스냅샷이 있으면 반환, 없으면 null (더미 데이터 생성 안 함)
    private Long findExistingDrivingScoreSnapshot(Long userId, Long userVehicleId) {
        return jdbcTemplate.query(
                """
                select id
                from driving_score_snapshots
                where user_id = ?
                  and user_vehicle_id = ?
                order by snapshot_date desc, id desc
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId,
                userVehicleId
        );
    }

    private Long insertInsuranceContract(
            Long userId,
            Long insuranceProductId,
            Long drivingScoreSnapshotId,
            String planType,
            LocalDate insuranceStartedAt,
            Integer annualPremium,
            LocalDateTime now
    ) {
        LocalDate endedDate = insuranceStartedAt.plusYears(1);
        String finalPlanType = planType == null ? DEFAULT_CONTRACT_PLAN_TYPE : planType;

        return insertAndReturnId(
                """
                insert into insurance_contracts
                (user_id, insurance_product_id, driving_score_snapshots_id, phone_number, address,
                 contract_period, plan_type, status, started_at, ended_at, created_at,
                 base_amount, discount_amount, discount_rate, final_amount)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                ps -> {
                    ps.setLong(1, userId);
                    ps.setLong(2, insuranceProductId);
                    ps.setObject(3, drivingScoreSnapshotId);
                    ps.setString(4, null);
                    ps.setString(5, null);
                    ps.setInt(6, 12);
                    ps.setString(7, finalPlanType);
                    ps.setString(8, DEFAULT_CONTRACT_STATUS);
                    ps.setTimestamp(9, Timestamp.valueOf(insuranceStartedAt.atStartOfDay()));
                    ps.setTimestamp(10, Timestamp.valueOf(endedDate.atStartOfDay()));
                    ps.setTimestamp(11, Timestamp.valueOf(now));
                    ps.setInt(12, annualPremium);
                    ps.setInt(13, 0);
                    ps.setBigDecimal(14, BigDecimal.ZERO);
                    ps.setInt(15, annualPremium);
                }
        );
    }

    private Long insertUserInsurance(
            Long userId,
            Long userVehicleId,
            Long insuranceCompanyId,
            Long insuranceProductId,
            Long insuranceContractId,
            LocalDateTime now
    ) {
        return insertAndReturnId(
                """
                insert into user_insurances
                (user_id, user_vehicle_id, insurance_company_id, insurance_product_id, insurance_contracts_id, status, created_at)
                values (?, ?, ?, ?, ?, ?, ?)
                """,
                ps -> {
                    ps.setLong(1, userId);
                    ps.setLong(2, userVehicleId);
                    ps.setLong(3, insuranceCompanyId);
                    ps.setLong(4, insuranceProductId);
                    ps.setLong(5, insuranceContractId);
                    ps.setString(6, "ACTIVE");
                    ps.setTimestamp(7, Timestamp.valueOf(now));
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

    public record InsuranceRegistrationIds(
            Long userInsuranceId,
            Long userVehicleId,
            Long insuranceCompanyId,
            Long insuranceContractId
    ) {
    }
}
