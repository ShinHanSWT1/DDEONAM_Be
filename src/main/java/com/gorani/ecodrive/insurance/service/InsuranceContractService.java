package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.email.EmailService;
import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.common.pdf.PdfService;
import com.gorani.ecodrive.driving.service.query.DrivingQueryService;
import com.gorani.ecodrive.insurance.controller.InsuranceContractController.CreateContractRequest;
import com.gorani.ecodrive.insurance.domain.*;
import com.gorani.ecodrive.insurance.repository.InsuranceCoverageRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceContractRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceProductRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceContractService {

    private final InsuranceContractRepository insuranceContractRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final InsuranceCoverageRepository insuranceCoverageRepository;
    private final DiscountCalculationService discountCalculationService;
    private final UserRepository userRepository;
    private final DrivingQueryService drivingQueryService;
    private final PdfService pdfService;
    private final EmailService emailService;

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

        // 4. planType 파싱
        InsurancePlanType planType = parsePlanType(request.planType());

        // 5. 필수 특약 확인 (planType 기준으로 검증)
        List<InsuranceCoverage> requiredCoverages = insuranceCoverageRepository
                .findAllByInsuranceProduct_IdAndPlanTypeAndStatus(
                        product.getId(), planType, InsuranceCoverageStatus.ACTIVE)
                .stream()
                .filter(InsuranceCoverage::getIsRequired)
                .toList();
        boolean allRequiredSelected = requiredCoverages.stream()
                .allMatch(c -> request.selectedCoverageIds().contains(c.getId()));
        if (!allRequiredSelected) {
            throw new CustomException(ErrorCode.REQUIRED_COVERAGE_MISSING);
        }

        // 6. 보험료 계산 (상품 base_amount × 나이보정 × 경력보정 × 안전점수 할인)
        int age = user.getAge() != null ? user.getAge() : 30;
        int score = java.util.Optional.ofNullable(drivingQueryService.getLatestScore(userId).score()).orElse(0);
        int experienceYears = insuranceContractRepository.findFirstByUser_IdOrderByStartedAtAsc(userId)
                .map(c -> c.getStartedAt() != null ? (int) ChronoUnit.YEARS.between(c.getStartedAt(), LocalDateTime.now()) : 100)
                .orElse(0);
        int baseAmount = product.getBaseAmount();
        int finalAmount = discountCalculationService.calculateFinalPremium(baseAmount, age, score, experienceYears);
        int adjustedBase = (int) (baseAmount * discountCalculationService.calculateAgeFactor(age)
                * discountCalculationService.calculateExperienceFactor(experienceYears));
        int discountAmount = adjustedBase - finalAmount;
        double discountRate = discountCalculationService.calculateScoreDiscountRate(age, score);

        // 7. 계약 생성
        InsuranceContract contract = InsuranceContract.builder()
                .user(user)
                .insuranceProduct(product)
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .contractPeriod(request.contractPeriod())
                .planType(planType)
                .status(InsuranceContractStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .baseAmount(baseAmount)
                .discountAmount(discountAmount)
                .discountRate(BigDecimal.valueOf(discountRate))
                .finalAmount(finalAmount)
                .build();

        InsuranceContract saved = insuranceContractRepository.save(contract);

        // 8. 선택된 특약 조회
        List<InsuranceCoverage> selectedCoverages = insuranceCoverageRepository
                .findAllById(request.selectedCoverageIds());

        // 9. PDF 생성 + 이메일 발송 (실패해도 계약은 유지)
        String email = request.email() != null && !request.email().isBlank()
                ? request.email()
                : user.getEmail();

        if (email != null && !email.isBlank()) {
            try {
                byte[] pdfBytes = pdfService.createContractPdf(
                        saved,
                        selectedCoverages,
                        request.signatureImage(),
                        user.getNickname()
                );
                emailService.sendContractEmail(email, user.getNickname(), pdfBytes);
            } catch (Exception e) {
                log.error("PDF 생성 또는 이메일 발송 실패 (계약은 정상 처리됨): {}", e.getMessage(), e);
            }
        }

        return saved;
    }

    public InsuranceContract getContract(Long contractId, Long userId) {
        InsuranceContract contract = insuranceContractRepository.findByIdAndUser_Id(contractId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_CONTRACT_NOT_FOUND));
        if (!contract.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.INSURANCE_CONTRACT_ACCESS_DENIED);
        }
        return contract;
    }

    public List<InsuranceContract> getMyContracts(Long userId, String status) {
        if (status != null) {
            return insuranceContractRepository.findAllByUser_IdAndStatus(
                    userId, parseStatus(status));
        }
        return insuranceContractRepository.findAllByUser_Id(userId);
    }

    private InsuranceContractStatus parseStatus(String raw) {
        try {
            return InsuranceContractStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private InsurancePlanType parsePlanType(String raw) {
        try {
            return InsurancePlanType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_PLAN_TYPE);
        }
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
