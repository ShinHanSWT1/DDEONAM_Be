package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.email.EmailService;
import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.common.pdf.PdfService;
import com.gorani.ecodrive.driving.service.query.DrivingQueryService;
import com.gorani.ecodrive.insurance.controller.InsuranceContractController.CreateContractRequest;
import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.domain.InsuranceContractStatus;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverage;
import com.gorani.ecodrive.insurance.domain.InsuranceCoverageStatus;
import com.gorani.ecodrive.insurance.domain.InsurancePlanType;
import com.gorani.ecodrive.insurance.domain.InsuranceProduct;
import com.gorani.ecodrive.insurance.domain.InsuranceProductStatus;
import com.gorani.ecodrive.insurance.domain.UserInsuranceStatus;
import com.gorani.ecodrive.insurance.repository.InsuranceCoverageRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceContractRepository;
import com.gorani.ecodrive.insurance.repository.InsuranceProductRepository;
import com.gorani.ecodrive.insurance.repository.UserInsuranceRepository;
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
import java.util.Optional;

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
    private final UserInsuranceRepository userInsuranceRepository;

    @Transactional
    public InsuranceContract createContract(Long userId, CreateContractRequest request) {
        InsuranceContract contract = createContractDraftInternal(userId, request);
        sendContractDocument(contract, request.selectedCoverageIds(), request.signatureImage(), request.email());
        return contract;
    }

    @Transactional
    public InsuranceContract createContractDraft(Long userId, CreateContractRequest request) {
        return createContractDraftInternal(userId, request);
    }

    public void sendContractDocument(
            InsuranceContract contract,
            List<Long> selectedCoverageIds,
            String signatureImage,
            String requestedEmail
    ) {
        User user = contract.getUser();
        String email = requestedEmail != null && !requestedEmail.isBlank()
                ? requestedEmail
                : user.getEmail();

        if (email == null || email.isBlank()) {
            return;
        }

        try {
            List<InsuranceCoverage> selectedCoverages = insuranceCoverageRepository.findAllById(selectedCoverageIds);

            int age = user.getAge() != null ? user.getAge() : 30;
            int experienceYears = insuranceContractRepository.findFirstByUser_IdOrderByStartedAtAsc(user.getId())
                    .filter(c -> c.getStartedAt() != null)
                    .map(c -> (int) ChronoUnit.YEARS.between(c.getStartedAt(), LocalDateTime.now()))
                    .orElse(0);

            double ageFactor = discountCalculationService.calculateAgeFactor(age);
            double experienceFactor = discountCalculationService.calculateExperienceFactor(experienceYears);

            byte[] pdfBytes = pdfService.createContractPdf(
                    contract,
                    selectedCoverages,
                    signatureImage,
                    user.getNickname(),
                    ageFactor,
                    experienceFactor
            );
            emailService.sendContractEmail(email, user.getNickname(), pdfBytes);
        } catch (Exception e) {
            log.error("PDF 생성 또는 이메일 발송 실패 (계약은 정상 처리). message={}", e.getMessage(), e);
        }
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

    @Transactional(readOnly = true)
    public PremiumEstimate estimatePremium(Long userId, Long productId, String planTypeStr) {
        return estimatePremium(userId, null, productId, planTypeStr);
    }

    public PremiumEstimate estimatePremium(Long userId, Long userVehicleId, Long productId, String planTypeStr) {
        InsuranceProduct product = insuranceProductRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        InsurancePlanType planType = parsePlanType(planTypeStr);
        int age = user.getAge() != null ? user.getAge() : 30;
        int score = Optional.ofNullable(drivingQueryService.getLatestScore(userId, userVehicleId).score()).orElse(0);
        int experienceYears = insuranceContractRepository.findFirstByUser_IdOrderByStartedAtAsc(userId)
                .filter(c -> c.getStartedAt() != null)
                .map(c -> (int) ChronoUnit.YEARS.between(c.getStartedAt(), LocalDateTime.now()))
                .orElse(0);
        int annualMileageKm = drivingQueryService.getAnnualDistanceKm(userId, userVehicleId);

        int rawBase = product.getBaseAmount();
        double planFactor = discountCalculationService.calculatePlanFactor(planType);
        int planAdjustedBase = (int) (rawBase * planFactor);
        double ageFactor = discountCalculationService.calculateAgeFactor(age);
        double experienceFactor = discountCalculationService.calculateExperienceFactor(experienceYears);
        int adjustedBase = (int) (planAdjustedBase * ageFactor * experienceFactor);
        double discountRate = discountCalculationService.calculateScoreDiscountRate(age, score, annualMileageKm);
        int finalAmount = (int) (adjustedBase * (1 - discountRate));
        int discountAmount = Math.max(0, adjustedBase - finalAmount);

        return new PremiumEstimate(
                product.getProductName(),
                planAdjustedBase,
                ageFactor,
                experienceFactor,
                adjustedBase,
                discountRate,
                discountAmount,
                finalAmount
        );
    }

    public record PremiumEstimate(
            String productName,
            int planAdjustedBase,
            double ageFactor,
            double experienceFactor,
            int adjustedBase,
            double discountRate,
            int discountAmount,
            int finalAmount
    ) {}

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

        userInsuranceRepository
                .findFirstByUser_IdAndInsuranceContract_IdAndStatus(
                        userId, contractId, UserInsuranceStatus.ACTIVE)
                .ifPresent(ui -> ui.deactivate(LocalDateTime.now()));
    }

    private InsuranceContract createContractDraftInternal(Long userId, CreateContractRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        InsuranceProduct product = insuranceProductRepository.findById(request.insuranceProductId())
                .orElseThrow(() -> new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_FOUND));

        if (product.getStatus() != InsuranceProductStatus.ON_SALE) {
            throw new CustomException(ErrorCode.INSURANCE_PRODUCT_NOT_ON_SALE);
        }

        InsurancePlanType planType = parsePlanType(request.planType());

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

        int age = user.getAge() != null ? user.getAge() : 30;
        int score = Optional.ofNullable(drivingQueryService.getLatestScore(userId, request.userVehicleId()).score()).orElse(0);
        int experienceYears = insuranceContractRepository.findFirstByUser_IdOrderByStartedAtAsc(userId)
                .filter(c -> c.getStartedAt() != null)
                .map(c -> (int) ChronoUnit.YEARS.between(c.getStartedAt(), LocalDateTime.now()))
                .orElse(0);
        int rawBaseAmount = product.getBaseAmount();
        double planFactor = discountCalculationService.calculatePlanFactor(planType);
        int planAdjustedBase = (int) (rawBaseAmount * planFactor);
        int annualMileageKm = drivingQueryService.getAnnualDistanceKm(userId, request.userVehicleId());
        double ageFactor = discountCalculationService.calculateAgeFactor(age);
        double experienceFactor = discountCalculationService.calculateExperienceFactor(experienceYears);
        int adjustedBase = (int) (planAdjustedBase * ageFactor * experienceFactor);
        double discountRate = discountCalculationService.calculateScoreDiscountRate(age, score, annualMileageKm);
        int finalAmount = (int) (adjustedBase * (1 - discountRate));
        int discountAmount = Math.max(0, adjustedBase - finalAmount);

        InsuranceContract contract = InsuranceContract.builder()
                .user(user)
                .insuranceProduct(product)
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .contractPeriod(request.contractPeriod())
                .planType(planType)
                .status(InsuranceContractStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .baseAmount(planAdjustedBase)
                .discountAmount(discountAmount)
                .discountRate(BigDecimal.valueOf(discountRate))
                .finalAmount(finalAmount)
                .build();

        return insuranceContractRepository.save(contract);
    }
}
