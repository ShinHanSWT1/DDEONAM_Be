package com.gorani.ecodrive.insurance.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.insurance.controller.InsuranceContractController.CreateContractRequest;
import com.gorani.ecodrive.insurance.domain.InsuranceCheckoutAttempt;
import com.gorani.ecodrive.insurance.domain.InsuranceContract;
import com.gorani.ecodrive.insurance.domain.UserInsurance;
import com.gorani.ecodrive.insurance.domain.UserInsuranceStatus;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutConfirmRequest;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutConfirmResponse;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutPrepareRequest;
import com.gorani.ecodrive.insurance.dto.InsuranceCheckoutPrepareResponse;
import com.gorani.ecodrive.insurance.repository.InsuranceCheckoutAttemptRepository;
import com.gorani.ecodrive.insurance.repository.UserInsuranceRepository;
import com.gorani.ecodrive.pay.dto.PayCheckoutSessionRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutSessionResponse;
import com.gorani.ecodrive.pay.service.PayIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InsuranceCheckoutService {

    private final InsuranceCheckoutAttemptRepository insuranceCheckoutAttemptRepository;
    private final InsuranceContractService insuranceContractService;
    private final UserInsuranceService userInsuranceService;
    private final UserInsuranceRepository userInsuranceRepository;
    private final PayIntegrationService payIntegrationService;

    @Transactional
    public InsuranceCheckoutPrepareResponse prepareCheckout(Long userId, InsuranceCheckoutPrepareRequest request) {
        boolean hasActiveInsurance = userInsuranceRepository
                .findFirstByUser_IdAndUserVehicleIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        request.userVehicleId(),
                        UserInsuranceStatus.ACTIVE
                )
                .isPresent();
        if (hasActiveInsurance) {
            // 프론트에서 기존 계약 해지 모달을 띄울 수 있도록 표준 에러코드로 반환
            throw new CustomException(ErrorCode.VEHICLE_ALREADY_HAS_ACTIVE_INSURANCE);
        }

        InsuranceContract contract = insuranceContractService.createContractDraft(
                userId,
                new CreateContractRequest(
                        request.insuranceProductId(),
                        request.userVehicleId(),
                        request.phoneNumber(),
                        request.address(),
                        request.contractPeriod(),
                        request.planType(),
                        request.selectedCoverageIds(),
                        request.signatureImage(),
                        request.email()
                )
        );

        int amount = contract.getFinalAmount();
        int pointAmount = resolvePointAmount(request.pointAmount(), amount);
        int finalAmount = amount - pointAmount;

        String orderId = buildInsuranceOrderId(userId);
        String title = buildCheckoutTitle(contract);

        PayCheckoutSessionResponse session = payIntegrationService.createCheckoutSession(
                userId,
                new PayCheckoutSessionRequest(
                        orderId,
                        title,
                        amount,
                        pointAmount,
                        0,
                        contract.getInsuranceProduct().getId(),
                        request.successUrl(),
                        request.failUrl(),
                        "MERCHANT_REDIRECT",
                        "REDIRECT"
                )
        );

        LocalDateTime now = LocalDateTime.now();
        InsuranceCheckoutAttempt attempt = insuranceCheckoutAttemptRepository.save(
                InsuranceCheckoutAttempt.prepare(
                        userId,
                        contract.getId(),
                        request.userVehicleId(),
                        request.insuranceProductId(),
                        orderId,
                        session.sessionToken(),
                        title,
                        amount,
                        pointAmount,
                        0,
                        finalAmount,
                        toCoverageCsv(request.selectedCoverageIds()),
                        request.signatureImage(),
                        request.email(),
                        session.expiresAt(),
                        now
                )
        );

        log.info("보험 checkout 준비 완료. userId={}, attemptId={}, contractId={}, orderId={}, amount={}, pointAmount={}, finalAmount={}",
                userId, attempt.getId(), contract.getId(), orderId, amount, pointAmount, finalAmount);

        return new InsuranceCheckoutPrepareResponse(
                contract.getId(),
                orderId,
                session.sessionToken(),
                session.checkoutUrl(),
                amount,
                pointAmount,
                finalAmount,
                session.expiresAt()
        );
    }

    @Transactional
    public InsuranceCheckoutConfirmResponse confirmCheckout(Long userId, InsuranceCheckoutConfirmRequest request) {
        InsuranceCheckoutAttempt attempt = insuranceCheckoutAttemptRepository.findByUserIdAndOrderId(userId, request.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 준비 정보를 찾을 수 없습니다."));

        if ("COMPLETED".equalsIgnoreCase(attempt.getStatus())) {
            if (attempt.getUserInsuranceId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 완료된 결제지만 보험 연결 정보가 없습니다.");
            }
            UserInsurance userInsurance = userInsuranceRepository.findById(attempt.getUserInsuranceId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "연결된 보험 정보를 찾을 수 없습니다."));

            return new InsuranceCheckoutConfirmResponse(
                    attempt.getOrderId(),
                    attempt.getPaymentId(),
                    userInsurance.getInsuranceContract().getId(),
                    userInsurance.getId(),
                    userInsurance.getInsuranceContract().getStatus().name()
            );
        }

        if (!"PREPARED".equalsIgnoreCase(attempt.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "확정 가능한 결제 상태가 아닙니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (attempt.isExpired(now)) {
            attempt.markFailed("결제 유효시간이 만료되었습니다.", now);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 유효시간이 만료되었습니다.");
        }

        boolean matchesAmount = request.amount().equals(attempt.getAmount()) || request.amount().equals(attempt.getFinalAmount());
        if (!matchesAmount) {
            attempt.markFailed("결제 금액 검증 실패", now);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }

        if (request.status() != null && !"COMPLETED".equalsIgnoreCase(request.status())) {
            attempt.markFailed("PAY 결제 상태가 COMPLETED가 아님", now);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제가 완료되지 않았습니다.");
        }

        UserInsurance userInsurance = userInsuranceService.confirmInsurance(
                userId,
                attempt.getInsuranceContractId(),
                attempt.getUserVehicleId()
        );

        InsuranceContract contract = userInsurance.getInsuranceContract();
        insuranceContractService.sendContractDocument(
                contract,
                parseCoverageIds(attempt.getSelectedCoverageIds()),
                attempt.getSignatureImage(),
                attempt.getEmail()
        );

        attempt.markCompleted(request.paymentId(), userInsurance.getId(), now);

        log.info("보험 checkout 확정 완료. userId={}, orderId={}, paymentId={}, contractId={}, userInsuranceId={}",
                userId, attempt.getOrderId(), request.paymentId(), contract.getId(), userInsurance.getId());

        return new InsuranceCheckoutConfirmResponse(
                attempt.getOrderId(),
                request.paymentId(),
                contract.getId(),
                userInsurance.getId(),
                contract.getStatus().name()
        );
    }

    private int resolvePointAmount(Integer pointAmount, int amount) {
        int resolved = pointAmount == null ? 0 : pointAmount;
        if (resolved < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트 사용 금액은 0 이상이어야 합니다.");
        }
        if (resolved > amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트 사용 금액은 결제 금액을 초과할 수 없습니다.");
        }
        return resolved;
    }

    private String buildInsuranceOrderId(Long userId) {
        return "ECO-INSURANCE-" + userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String buildCheckoutTitle(InsuranceContract contract) {
        String companyName = contract.getInsuranceProduct().getInsuranceCompany().getCompanyName();
        String productName = contract.getInsuranceProduct().getProductName();
        return companyName + " " + productName + " 보험료";
    }

    private String toCoverageCsv(List<Long> coverageIds) {
        return coverageIds.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private List<Long> parseCoverageIds(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
