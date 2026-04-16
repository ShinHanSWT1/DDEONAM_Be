package com.gorani.ecodrive.user.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.driving.service.aggregation.DrivingSnapshotInitializationService;
import com.gorani.ecodrive.insurance.service.InsuranceOnboardingService;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import com.gorani.ecodrive.vehicle.service.VehicleOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final VehicleOnboardingService vehicleOnboardingService;
    private final InsuranceOnboardingService insuranceOnboardingService;
    private final DrivingSnapshotInitializationService drivingSnapshotInitializationService;

    @Transactional
    public VehicleRegistrationResult registerVehicle(Long userId, VehicleRegistrationRequest request) {
        String vehicleNumber = validateRequiredText(request.vehicleNumber());
        Long vehicleModelId = validateRequiredId(request.vehicleModelId());

        User user = getUser(userId);
        LocalDateTime now = LocalDateTime.now();

        Long userVehicleId = vehicleOnboardingService.registerVehicle(
                user.getId(),
                vehicleModelId,
                vehicleNumber,
                now
        );
        user.updateRepresentativeUserVehicleId(userVehicleId);
        drivingSnapshotInitializationService.initializeDefaults(
                user.getId(),
                userVehicleId,
                now.toLocalDate(),
                now
        );

        boolean isOnboardingCompleted = refreshOnboardingCompleted(user);

        return new VehicleRegistrationResult(
                userVehicleId,
                vehicleNumber,
                vehicleModelId,
                isOnboardingCompleted
        );
    }

    @Transactional
    public InsuranceRegistrationResult registerInsurance(Long userId, InsuranceRegistrationRequest request) {
        String insuranceCompanyName = validateRequiredText(request.insuranceCompanyName());
        validatePositive(request.annualPremium());
        validatePositive(request.age());
        validateRequiredId(request.userVehicleId());
        if (request.insuranceStartedAt() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = getUser(userId);
        user.updateAge(request.age());

        InsuranceOnboardingService.InsuranceRegistrationIds insuranceRegistrationIds =
                insuranceOnboardingService.registerInsurance(
                        user.getId(),
                        request.userVehicleId(),
                        insuranceCompanyName,
                        request.insuranceProductName(),
                        request.planType(),
                        request.annualPremium(),
                        request.insuranceStartedAt(),
                        LocalDateTime.now()
                );

        boolean isOnboardingCompleted = refreshOnboardingCompleted(user);

        return new InsuranceRegistrationResult(
                insuranceRegistrationIds.userInsuranceId(),
                insuranceRegistrationIds.userVehicleId(),
                insuranceRegistrationIds.insuranceCompanyId(),
                insuranceRegistrationIds.insuranceContractId(),
                isOnboardingCompleted
        );
    }

    @Transactional
    public OnboardingCompletionResult completeOnboarding(Long userId) {
        User user = getUser(userId);
        boolean isOnboardingCompleted = refreshOnboardingCompleted(user);

        return new OnboardingCompletionResult(isOnboardingCompleted);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean refreshOnboardingCompleted(User user) {
        boolean isOnboardingCompleted = userService.calculateOnboardingCompleted(user.getId());
        user.updateOnboardingCompleted(isOnboardingCompleted);
        return isOnboardingCompleted;
    }

    private String validateRequiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return value.trim();
    }

    private Long validateRequiredId(Long value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return value;
    }

    private void validatePositive(Integer value) {
        if (value == null || value <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
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
            String planType,
            Integer annualPremium,
            LocalDate insuranceStartedAt,
            Integer age
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
