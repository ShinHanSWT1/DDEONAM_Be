package com.gorani.ecodrive.user.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String DEFAULT_INSURANCE_PRODUCT_NAME = "온보딩 표준형";
    private static final String DEFAULT_CONTRACT_STATUS = "ACTIVE";
    private static final String DEFAULT_CONTRACT_PLAN_TYPE = "STANDARD";
    private static final int DEFAULT_DRIVING_SCORE = 100;

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public VehicleRegistrationResult registerVehicle(Long userId, VehicleRegistrationRequest request) {
        validateText(request.vehicleNumber(), "차량 번호");
        if (request.vehicleModelId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        Long vehicleModelId = findVehicleModelId(request.vehicleModelId());
        Long userVehicleId = insertUserVehicle(user.getId(), vehicleModelId, request.vehicleNumber().trim(), now);
        boolean isOnboardingCompleted = userService.calculateOnboardingCompleted(userId);

        user.updateOnboardingCompleted(isOnboardingCompleted);

        return new VehicleRegistrationResult(
                userVehicleId,
                request.vehicleNumber().trim(),
                vehicleModelId,
                isOnboardingCompleted
        );
    }

    @Transactional
    public InsuranceRegistrationResult registerInsurance(Long userId, InsuranceRegistrationRequest request) {
        validateText(request.insuranceCompanyName(), "보험사명");
        validatePositive(request.annualPremium(), "연간 보험료");
        if (request.insuranceStartedAt() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Long userVehicleId = resolveUserVehicleId(userId, request.userVehicleId());
        LocalDateTime now = LocalDateTime.now();
        Long insuranceCompanyId = findOrCreateInsuranceCompany(request.insuranceCompanyName(), now);
        Long insuranceProductId = findOrCreateInsuranceProduct(
                insuranceCompanyId,
                request.insuranceProductName(),
                request.annualPremium(),
                now
        );
        Long drivingScoreSnapshotId = findOrCreateDrivingScoreSnapshot(userId, now.toLocalDate(), now);
        Long insuranceContractId = insertInsuranceContract(
                user.getId(),
                insuranceProductId,
                drivingScoreSnapshotId,
                request.insuranceStartedAt(),
                request.annualPremium(),
                now
        );
        Long userInsuranceId = insertUserInsurance(
                user.getId(),
                userVehicleId,
                insuranceCompanyId,
                insuranceProductId,
                insuranceContractId,
                now
        );

        boolean isOnboardingCompleted = userService.calculateOnboardingCompleted(userId);
        user.updateOnboardingCompleted(isOnboardingCompleted);

        return new InsuranceRegistrationResult(
                userInsuranceId,
                userVehicleId,
                insuranceCompanyId,
                insuranceContractId,
                isOnboardingCompleted
        );
    }

    @Transactional
    public OnboardingCompletionResult completeOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean isOnboardingCompleted = userService.calculateOnboardingCompleted(userId);
        user.updateOnboardingCompleted(isOnboardingCompleted);

        return new OnboardingCompletionResult(isOnboardingCompleted);
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

    private Long resolveUserVehicleId(Long userId, Long requestedUserVehicleId) {
        if (requestedUserVehicleId != null) {
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

            if (existing != null) {
                return existing;
            }
        }

        Long latestUserVehicleId = jdbcTemplate.query(
                """
                select id
                from user_vehicles
                where user_id = ?
                order by id desc
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId
        );

        if (latestUserVehicleId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return latestUserVehicleId;
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

    private Long findOrCreateDrivingScoreSnapshot(Long userId, LocalDate snapshotDate, LocalDateTime now) {
        Long existingId = jdbcTemplate.query(
                """
                select id
                from driving_score_snapshots
                where user_id = ?
                order by snapshot_date desc, id desc
                limit 1
                """,
                rs -> rs.next() ? rs.getLong("id") : null,
                userId
        );

        if (existingId != null) {
            return existingId;
        }

        return insertAndReturnId(
                """
                insert into driving_score_snapshots
                (user_id, snapshot_date, score, created_at)
                values (?, ?, ?, ?)
                """,
                ps -> {
                    ps.setLong(1, userId);
                    ps.setDate(2, Date.valueOf(snapshotDate));
                    ps.setInt(3, DEFAULT_DRIVING_SCORE);
                    ps.setTimestamp(4, Timestamp.valueOf(now));
                }
        );
    }

    private Long insertInsuranceContract(
            Long userId,
            Long insuranceProductId,
            Long drivingScoreSnapshotId,
            LocalDate insuranceStartedAt,
            Integer annualPremium,
            LocalDateTime now
    ) {
        LocalDate endedDate = insuranceStartedAt.plusYears(1);

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
                    ps.setLong(3, drivingScoreSnapshotId);
                    ps.setString(4, null);
                    ps.setString(5, null);
                    ps.setInt(6, 12);
                    ps.setString(7, DEFAULT_CONTRACT_PLAN_TYPE);
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
                (user_id, user_vehicle_id, insurance_company_id, insurance_product_id, insurance_contracts_id, created_at)
                values (?, ?, ?, ?, ?, ?)
                """,
                ps -> {
                    ps.setLong(1, userId);
                    ps.setLong(2, userVehicleId);
                    ps.setLong(3, insuranceCompanyId);
                    ps.setLong(4, insuranceProductId);
                    ps.setLong(5, insuranceContractId);
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

    private void validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement preparedStatement) throws java.sql.SQLException;
    }

    public record VehicleRegistrationRequest(
            String vehicleNumber,
            Long vehicleModelId
    ) {
    }

    public record VehicleRegistrationResult(
            Long userVehicleId,
            String vehicleNumber,
            Long vehicleModelId,
            boolean isOnboardingCompleted
    ) {
    }

    public record InsuranceRegistrationRequest(
            Long userVehicleId,
            String insuranceCompanyName,
            String insuranceProductName,
            Integer annualPremium,
            LocalDate insuranceStartedAt
    ) {
    }

    public record InsuranceRegistrationResult(
            Long userInsuranceId,
            Long userVehicleId,
            Long insuranceCompanyId,
            Long insuranceContractId,
            boolean isOnboardingCompleted
    ) {
    }

    public record OnboardingCompletionResult(
            boolean isOnboardingCompleted
    ) {
    }
}
