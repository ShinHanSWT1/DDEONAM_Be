package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.domain.InsuranceContractStatus;
import com.gorani.ecodrive.insurance.domain.UserInsurance;
import com.gorani.ecodrive.insurance.domain.UserInsuranceStatus;
import com.gorani.ecodrive.insurance.repository.UserInsuranceRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import com.gorani.ecodrive.vehicle.repository.UserVehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInsuranceService {

    private final UserInsuranceRepository userInsuranceRepository;
    private final UserRepository userRepository;
    private final InsuranceContractService insuranceContractService;
    private final UserVehicleRepository userVehicleRepository;

    public List<UserInsurance> getMyInsurances(Long userId) {
        return userInsuranceRepository.findAllByUser_Id(userId);
    }

    public UserInsurance getMyInsurance(Long insuranceId, Long userId) {
        return userInsuranceRepository.findByIdAndUser_Id(insuranceId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_INSURANCE_NOT_FOUND));
    }

    @Transactional
    public UserInsurance confirmInsurance(Long userId, Long contractId, Long userVehicleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!userVehicleRepository.existsByIdAndUser_Id(userVehicleId, userId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 해당 차량에 이미 ACTIVE 보험이 있으면 먼저 해지해야 함
        boolean hasActiveInsurance = userInsuranceRepository
                .findFirstByUser_IdAndUserVehicleIdAndStatusOrderByCreatedAtDesc(
                        userId, userVehicleId, UserInsuranceStatus.ACTIVE)
                .isPresent();
        if (hasActiveInsurance) {
            throw new CustomException(ErrorCode.VEHICLE_ALREADY_HAS_ACTIVE_INSURANCE);
        }

        InsuranceContract contract = insuranceContractService.getContract(contractId, userId);

        InsuranceContractStatus status = contract.getStatus();
        if (status == InsuranceContractStatus.ACTIVE) {
            throw new CustomException(ErrorCode.CONTRACT_ALREADY_ACTIVE);
        } else if (status == InsuranceContractStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ALREADY_CANCELLED);
        } else if (status != InsuranceContractStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_CONTRACT_STATUS);
        }

        contract.activate();

        UserInsurance userInsurance = UserInsurance.builder()
                .user(user)
                .userVehicleId(userVehicleId)
                .insuranceCompany(contract.getInsuranceProduct().getInsuranceCompany())
                .insuranceProduct(contract.getInsuranceProduct())
                .insuranceContract(contract)
                .status(UserInsuranceStatus.ACTIVE)
                .endedAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        return userInsuranceRepository.save(userInsurance);
    }
}
