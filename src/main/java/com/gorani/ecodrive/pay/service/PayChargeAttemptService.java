package com.gorani.ecodrive.pay.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.pay.domain.PayChargeAttempt;
import com.gorani.ecodrive.pay.domain.PayChargeAttemptStatus;
import com.gorani.ecodrive.pay.dto.PayChargePrepareResponse;
import com.gorani.ecodrive.pay.repository.PayChargeAttemptRepository;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayChargeAttemptService {

    private static final long CHARGE_ATTEMPT_EXPIRE_MINUTES = 30L;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    private final PayChargeAttemptRepository payChargeAttemptRepository;
    private final UserService userService;

    @Transactional
    public PayChargePrepareResponse prepare(Long userId, Integer amount) {
        User user = userService.getById(userId);
        String orderId = buildOrderId(userId);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(CHARGE_ATTEMPT_EXPIRE_MINUTES);

        PayChargeAttempt attempt = PayChargeAttempt.prepare(user, orderId, amount, expiresAt);
        payChargeAttemptRepository.save(attempt);

        return new PayChargePrepareResponse(orderId, amount, expiresAt);
    }

    public PayChargeAttempt requireConfirmable(Long userId, String orderId, Integer amount, String paymentKey) {
        PayChargeAttempt attempt = payChargeAttemptRepository.findByUserIdAndOrderId(userId, orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAY_CHARGE_ATTEMPT_NOT_FOUND));

        if (attempt.getAmount() == null || !attempt.getAmount().equals(amount)) {
            throw new CustomException(ErrorCode.PAY_CHARGE_AMOUNT_MISMATCH);
        }

        if (attempt.isExpired(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.PAY_CHARGE_ATTEMPT_EXPIRED);
        }

        if (attempt.getStatus() == PayChargeAttemptStatus.EXPIRED) {
            throw new CustomException(ErrorCode.PAY_CHARGE_ATTEMPT_EXPIRED);
        }

        if (attempt.getStatus() == PayChargeAttemptStatus.CONFIRMED) {
            if (attempt.getPaymentKey() != null && !attempt.getPaymentKey().equals(paymentKey)) {
                throw new CustomException(ErrorCode.PAY_CHARGE_DUPLICATED_CONFIRM);
            }
            return attempt;
        }

        if (attempt.getStatus() == PayChargeAttemptStatus.FAILED && attempt.getPaymentKey() != null
                && !attempt.getPaymentKey().equals(paymentKey)) {
            throw new CustomException(ErrorCode.PAY_CHARGE_DUPLICATED_CONFIRM);
        }

        return attempt;
    }

    @Transactional
    public void markConfirmed(PayChargeAttempt attempt, String paymentKey) {
        attempt.markConfirmed(paymentKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(PayChargeAttempt attempt, String errorCode, String errorMessage) {
        PayChargeAttempt managed = payChargeAttemptRepository.findById(attempt.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.PAY_CHARGE_ATTEMPT_NOT_FOUND));
        String normalizedErrorMessage = normalizeErrorMessage(errorMessage);
        managed.markFailed(errorCode, normalizedErrorMessage);
    }

    // DB 저장용 실패 메시지는 길이를 제한해 DataIntegrityViolationException을 방지한다.
    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        if (errorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        log.warn("충전 실패 메시지 길이 초과로 저장용 메시지를 잘라냅니다. originalLength={}, maxLength={}",
                errorMessage.length(), MAX_ERROR_MESSAGE_LENGTH);
        return errorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    @Transactional
    public int expirePreparedAttempts() {
        List<PayChargeAttempt> expiredTargets = payChargeAttemptRepository.findByStatusAndExpiresAtBefore(
                PayChargeAttemptStatus.PREPARED,
                LocalDateTime.now()
        );

        for (PayChargeAttempt attempt : expiredTargets) {
            attempt.markExpired();
        }

        return expiredTargets.size();
    }

    private String buildOrderId(Long userId) {
        return "ECO-CHARGE-" + userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
