package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.controller.InsuranceContractController.CreateContractRequest;
import com.gorani.ecodrive.insurance.domain.*;
import com.gorani.ecodrive.insurance.repository.InsuranceCoverageRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceContractRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceProductRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceContractService {

    private final InsuranceContractRepository insuranceContractRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final InsuranceCoverageRepository insuranceCoverageRepository;
    private final DiscountCalculationService discountCalculationService;
    private final UserRepository userRepository;

    @Transactional
    public InsuranceContract createContract(Long userId, CreateContractRequest request) {
        // 1. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 상품 조회
        InsuranceProduct product = insuranceProductRepository.findById(request.insuranceProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

        // 3. 판매 중 확인
        if (product.getStatus() != InsuranceProductStatus.ON_SALE) {
            throw new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_ON_SALE);
        }

        // 4. 필수 특약 확인
        List<InsuranceCoverage> requiredCoverages = insuranceCoverageRepository
                .findAllByInsuranceProduct_IdAndIsRequiredAndStatus(
                        product.getId(), true, InsuranceCoverageStatus.ACTIVE);
        boolean allRequiredSelected = requiredCoverages.stream()
                .allMatch(c -> request.selectedCoverageIds().contains(c.getId()));
        if (!allRequiredSelected) {
            throw new CustomException(ErrorCode.REQUIRED_COVERAGE_MISSING);
        }

        // 5. 보험료 계산 (임시 고정값 - DrivingScoreSnapshot 연동 후 교체 예정)
        int age = user.getAge() != null ? user.getAge() : 30;
        int score = 92; // TODO: DrivingScoreSnapshot에서 가져오기
        int experienceYears = 3; // TODO: 운전 경력 계산 로직 연동
        int basePremium = discountCalculationService.calculateBasePremium(age);
        int finalAmount = discountCalculationService.calculateFinalPremium(age, score, experienceYears);
        int discountAmount = basePremium - finalAmount;
        double discountRate = discountCalculationService.calculateScoreDiscountRate(age, score);

        // 6. 계약 생성
        InsuranceContract contract = InsuranceContract.builder()
                .user(user)
                .insuranceProduct(product)
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .contractPeriod(request.contractPeriod())
                .planType(InsurancePlanType.valueOf(request.planType()))
                .status(InsuranceContractStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .baseAmount(basePremium)
                .discountAmount(discountAmount)
                .discountRate(BigDecimal.valueOf(discountRate))
                .finalAmount(finalAmount)
                .build();

        return insuranceContractRepository.save(contract);
    }

    public InsuranceContract getContract(Long contractId, Long userId) {
        InsuranceContract contract = insuranceContractRepository.findById(contractId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_CONTRACT_NOT_FOUND));
        if (!contract.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.INSURANCE_CONTRACT_ACCESS_DENIED);
        }
        return contract;
    }

    public List<InsuranceContract> getMyContracts(Long userId, String status) {
        if (status != null) {
            return insuranceContractRepository.findAllByUser_IdAndStatus(
                    userId, InsuranceContractStatus.valueOf(status));
        }
        return insuranceContractRepository.findAllByUser_Id(userId);
    }

    @Transactional
    public void cancelContract(Long contractId, Long userId) {
        InsuranceContract contract = getContract(contractId, userId);
        if (contract.getStatus() == InsuranceContractStatus.CANCELLED) {
            throw new CustomException(ErrorCode.ALREADY_CANCELLED);
        }
        contract.cancel();
    }
}
