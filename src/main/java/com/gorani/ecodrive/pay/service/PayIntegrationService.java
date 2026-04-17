package com.gorani.ecodrive.pay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.pay.client.GoraniPayClient;
import com.gorani.ecodrive.pay.domain.PayChargeAttempt;
import com.gorani.ecodrive.pay.domain.PayChargeAttemptStatus;
import com.gorani.ecodrive.pay.dto.PayChargeConfirmRequest;
import com.gorani.ecodrive.pay.dto.PayChargePrepareRequest;
import com.gorani.ecodrive.pay.dto.PayChargePrepareResponse;
import com.gorani.ecodrive.pay.dto.PayChargeRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutResponse;
import com.gorani.ecodrive.pay.dto.PayCheckoutSessionRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutSessionResponse;
import com.gorani.ecodrive.pay.dto.PayTransactionResponse;
import com.gorani.ecodrive.pay.dto.PayWalletResponse;
import com.gorani.ecodrive.user.domain.User;
import com.gorani.ecodrive.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PayIntegrationService {

    private static final DateTimeFormatter TRANSACTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GoraniPayClient goraniPayClient;
    private final UserService userService;
    private final PayChargeAttemptService payChargeAttemptService;

    public PayWalletResponse getWallet(Long userId) {
        log.info("Pay 지갑 조회 시작. userId={}", userId);
        try {
            GoraniPayClient.PayAccountPayload account = goraniPayClient.getAccountByExternalUserId(userId);
            log.info("Pay 지갑 조회 완료. userId={}, payUserId={}, payAccountId={}, balance={}",
                    userId, account.payUserId(), account.id(), account.balance());
            return toWalletResponse(account);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Pay 지갑 미연결 사용자. userId={}", userId);
            throw new CustomException(ErrorCode.PAY_ACCOUNT_NOT_FOUND);
        } catch (RestClientResponseException e) {
            log.error("Pay 지갑 조회 연동 오류(응답). userId={}, statusCode={}, responseBody={}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (ResourceAccessException e) {
            log.error("Pay 지갑 조회 연동 오류(네트워크). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (RestClientException e) {
            log.error("Pay 지갑 조회 연동 오류(클라이언트). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        }
    }

    @Transactional
    public PayWalletResponse createWallet(Long userId) {
        log.info("Pay 지갑 생성 시작. userId={}", userId);
        try {
            User user = userService.getById(userId);
            GoraniPayClient.PayAccountPayload account = goraniPayClient.createAccount(
                    new GoraniPayClient.CreateAccountPayload(
                            userId,
                            resolveUserName(user),
                            resolveEmail(userId, user),
                            resolveOwnerName(user),
                            null,
                            null
                    )
            );
            log.info("Pay 지갑 생성 완료. userId={}, payUserId={}, payAccountId={}, balance={}",
                    userId, account.payUserId(), account.id(), account.balance());
            return toWalletResponse(account);
        } catch (RestClientResponseException e) {
            log.error("Pay 지갑 생성 연동 오류(응답). userId={}, statusCode={}, responseBody={}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (ResourceAccessException e) {
            log.error("Pay 지갑 생성 연동 오류(네트워크). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (RestClientException e) {
            log.error("Pay 지갑 생성 연동 오류(클라이언트). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        }
    }

    @Transactional
    public PayChargePrepareResponse prepareCharge(Long userId, PayChargePrepareRequest request) {
        log.info("Pay 충전 준비 시작. userId={}, amount={}", userId, request.amount());
        // 지갑이 없는 사용자는 결제창 진입 전에 차단
        getWallet(userId);
        PayChargePrepareResponse response = payChargeAttemptService.prepare(userId, request.amount());
        log.info("Pay 충전 준비 완료. userId={}, orderId={}, amount={}, expiresAt={}",
                userId, response.orderId(), response.amount(), response.expiresAt());
        return response;
    }

    @Transactional
    public PayWalletResponse charge(Long userId, PayChargeRequest request) {
        log.info("Pay 충전 시작. userId={}, amount={}", userId, request.amount());
        PayWalletResponse wallet = getWallet(userId);
        GoraniPayClient.PayAccountPayload charged = goraniPayClient.charge(wallet.payUserId(), request.amount());
        log.info("Pay 충전 완료. userId={}, payUserId={}, payAccountId={}, balance={}",
                userId, charged.payUserId(), charged.id(), charged.balance());
        return toWalletResponse(charged);
    }

    @Transactional
    public PayWalletResponse confirmCharge(Long userId, PayChargeConfirmRequest request) {
        log.info("Pay 토스 충전 승인 시작. userId={}, orderId={}, amount={}", userId, request.orderId(), request.amount());
        PayChargeAttempt attempt = payChargeAttemptService.requireConfirmable(
                userId,
                request.orderId(),
                request.amount(),
                request.paymentKey()
        );

        if (attempt.getStatus() == PayChargeAttemptStatus.CONFIRMED) {
            log.info("이미 확정된 충전 요청입니다. userId={}, orderId={}", userId, request.orderId());
            return getWallet(userId);
        }

        try {
            PayWalletResponse wallet = getWallet(userId);
            GoraniPayClient.PayAccountPayload confirmed = goraniPayClient.confirmCharge(
                    wallet.payUserId(),
                    request.paymentKey(),
                    request.orderId(),
                    request.amount()
            );
            payChargeAttemptService.markConfirmed(attempt, request.paymentKey());

            log.info("Pay 토스 충전 승인 완료. userId={}, payUserId={}, payAccountId={}, balance={}",
                    userId, confirmed.payUserId(), confirmed.id(), confirmed.balance());
            return toWalletResponse(confirmed);
        } catch (RestClientResponseException e) {
            payChargeAttemptService.markFailed(
                    attempt,
                    "PAY_CONFIRM_RESPONSE_ERROR",
                    extractConfirmErrorMessage(e.getResponseBodyAsString())
            );
            log.error("Pay 토스 충전 승인 연동 오류(응답). userId={}, statusCode={}, responseBody={}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (ResourceAccessException e) {
            payChargeAttemptService.markFailed(attempt, "PAY_CONFIRM_NETWORK_ERROR", e.getMessage());
            log.error("Pay 토스 충전 승인 연동 오류(네트워크). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (RestClientException e) {
            payChargeAttemptService.markFailed(attempt, "PAY_CONFIRM_CLIENT_ERROR", e.getMessage());
            log.error("Pay 토스 충전 승인 연동 오류(클라이언트). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        }
    }

    @Transactional
    public PayCheckoutResponse checkout(Long userId, PayCheckoutRequest request) {
        log.info("Pay 결제 시작. userId={}, title={}, amount={}", userId, request.title(), request.amount());
        PayWalletResponse wallet = getWallet(userId);

        String externalOrderId = buildExternalOrderId(userId);
        GoraniPayClient.PayPaymentPayload created = goraniPayClient.createPayment(
                new GoraniPayClient.CreatePaymentPayload(
                        wallet.payUserId(),
                        wallet.payAccountId(),
                        externalOrderId,
                        resolvePaymentType(request.paymentType()),
                        request.payProductId(),
                        request.title(),
                        request.amount(),
                        resolveNonNegativeAmount(request.pointAmount()),
                        resolveNonNegativeAmount(request.couponDiscountAmount())
                ),
                UUID.randomUUID().toString()
        );
        log.info("Pay 결제 생성 완료. userId={}, paymentId={}, externalOrderId={}, status={}",
                userId, created.id(), created.externalOrderId(), created.status());

        GoraniPayClient.PayPaymentPayload completed = goraniPayClient.completePayment(
                created.id(),
                UUID.randomUUID().toString()
        );
        GoraniPayClient.PayAccountPayload accountAfterPayment = goraniPayClient.getAccount(wallet.payUserId());
        log.info("Pay 결제 완료. userId={}, paymentId={}, status={}, balanceAfterPayment={}",
                userId, completed.id(), completed.status(), accountAfterPayment.balance());

        return new PayCheckoutResponse(
                completed.id(),
                completed.externalOrderId(),
                completed.status(),
                completed.amount(),
                completed.paymentType(),
                completed.payUserId(),
                completed.payAccountId(),
                accountAfterPayment.balance()
        );
    }

    @Transactional
    public PayCheckoutSessionResponse createCheckoutSession(Long userId, PayCheckoutSessionRequest request) {
        log.info("Pay checkout session 생성 시작. userId={}, title={}, amount={}, pointAmount={}, couponDiscountAmount={}",
                userId, request.title(), request.amount(), request.pointAmount(), request.couponDiscountAmount());

        // EcoDrive 사용자 -> Pay 사용자 매핑 및 지갑 유효성 검증
        PayWalletResponse wallet = getWallet(userId);
        String externalOrderId = StringUtils.hasText(request.externalOrderId())
                ? request.externalOrderId()
                : buildExternalOrderId(userId);

        // 실제 결제는 Pay Hosted Checkout에서 수행하므로 BE는 세션 발급만 위임
        GoraniPayClient.PayCheckoutSessionPayload payload = goraniPayClient.createCheckoutSession(
                new GoraniPayClient.CreateCheckoutSessionPayload(
                        "ECODRIVE",
                        wallet.payUserId(),
                        externalOrderId,
                        request.title(),
                        request.amount(),
                        resolveNonNegativeAmount(request.pointAmount()),
                        resolveNonNegativeAmount(request.couponDiscountAmount()),
                        request.payProductId(),
                        request.successUrl(),
                        request.failUrl(),
                        request.entryMode(),
                        request.channel()
                )
        );

        log.info("Pay checkout session 생성 완료. userId={}, externalOrderId={}, sessionToken={}, checkoutUrl={}",
                userId, externalOrderId, payload.sessionToken(), payload.checkoutUrl());

        return new PayCheckoutSessionResponse(
                payload.sessionToken(),
                payload.checkoutUrl(),
                payload.status(),
                payload.expiresAt()
        );
    }

    @Transactional
    public PayWalletResponse earnRewardPoints(
            Long userId,
            Integer amount,
            String category,
            String description,
            String externalOrderId,
            String idempotencyKey
    ) {
        log.info("Pay 리워드 포인트 적립 시작. userId={}, amount={}, category={}, externalOrderId={}",
                userId, amount, category, externalOrderId);
        try {
            PayWalletResponse wallet = getWallet(userId);
            GoraniPayClient.PayAccountPayload updated = goraniPayClient.earnPoints(
                    new GoraniPayClient.EarnPointPayload(
                            wallet.payUserId(),
                            resolveNonNegativeAmount(amount),
                            category,
                            description,
                            externalOrderId
                    ),
                    idempotencyKey
            );
            log.info("Pay 리워드 포인트 적립 완료. userId={}, payUserId={}, payAccountId={}, points={}",
                    userId, updated.payUserId(), updated.id(), updated.points());
            return toWalletResponse(updated);
        } catch (RestClientResponseException e) {
            log.error("Pay 리워드 포인트 적립 연동 오류(응답). userId={}, statusCode={}, responseBody={}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (ResourceAccessException e) {
            log.error("Pay 리워드 포인트 적립 연동 오류(네트워크). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        } catch (RestClientException e) {
            log.error("Pay 리워드 포인트 적립 연동 오류(클라이언트). userId={}, message={}", userId, e.getMessage(), e);
            throw new CustomException(ErrorCode.PAY_INTEGRATION_FAILED);
        }
    }

    public List<PayTransactionResponse> getTransactions(Long userId) {
        log.info("Pay 거래내역 조회 시작. userId={}", userId);
        PayWalletResponse wallet = getWallet(userId);

        List<PayTransactionResponse> transactions = goraniPayClient.getTransactions(wallet.payUserId())
                .stream()
                .map(this::toTransactionResponse)
                .toList();

        log.info("Pay 거래내역 조회 완료. userId={}, size={}", userId, transactions.size());
        return transactions;
    }

    private PayWalletResponse toWalletResponse(GoraniPayClient.PayAccountPayload account) {
        return new PayWalletResponse(
                account.payUserId(),
                account.id(),
                account.accountNumber(),
                account.bankCode(),
                account.ownerName(),
                account.balance(),
                account.points(),
                account.status()
        );
    }

    private String buildExternalOrderId(Long userId) {
        return "ECO-" + userId + "-" + System.currentTimeMillis();
    }

    private String resolvePaymentType(String paymentType) {
        return StringUtils.hasText(paymentType) ? paymentType : "WALLET";
    }

    private String resolveUserName(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return "user-" + user.getId();
    }

    private String resolveOwnerName(User user) {
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return "user-" + user.getId();
    }

    private String resolveEmail(Long userId, User user) {
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail();
        }
        return "user" + userId + "@ecodrive.local";
    }

    private Integer resolveNonNegativeAmount(Integer amount) {
        if (amount == null || amount < 0) {
            return 0;
        }
        return amount;
    }

    private PayTransactionResponse toTransactionResponse(GoraniPayClient.PayTransactionPayload payload) {
        String type = "CREDIT".equalsIgnoreCase(payload.direction()) ? "earn" : "pay";
        String title = resolveTransactionTitle(payload.transactionType());
        String category = StringUtils.hasText(payload.category())
                ? resolveTransactionCategory(payload.category())
                : ("earn".equals(type) ? "충전" : "결제");
        String date = payload.occurredAt() != null
                ? payload.occurredAt().format(TRANSACTION_DATE_FORMATTER)
                : "";

        return new PayTransactionResponse(
                payload.id(),
                payload.transactionType(),
                title,
                date,
                payload.amount(),
                type,
                category
        );
    }

    private String resolveTransactionTitle(String transactionType) {
        if (!StringUtils.hasText(transactionType)) {
            return "거래";
        }

        return switch (transactionType) {
            case "CHARGE" -> "지갑 충전";
            case "WITHDRAW" -> "출금";
            case "PAYMENT" -> "결제";
            case "REFUND" -> "환불";
            case "POINT_EARN" -> "포인트 적립";
            case "POINT_USE" -> "포인트 사용";
            default -> transactionType;
        };
    }

    // pay 서비스 분류 코드를 FE 표시용 한글 라벨로 정규화한다.
    private String resolveTransactionCategory(String category) {
        return switch (category) {
            case "COUPON" -> "쿠폰 결제";
            case "GENERAL" -> "일반 결제";
            case "POINT" -> "포인트";
            case "MISSION" -> "미션 보상";
            case "CARBON" -> "탄소 리워드";
            default -> category;
        };
    }

    // 승인 실패 시 전체 응답 대신 message + body.message만 저장
    private String extractConfirmErrorMessage(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            String message = root.path("message").asText(null);
            if (!StringUtils.hasText(message)) {
                return responseBody;
            }

            int bodyIndex = message.indexOf("body=");
            if (bodyIndex < 0) {
                return message;
            }

            String outerMessage = message.substring(0, bodyIndex).trim();
            String bodyJson = message.substring(bodyIndex + 5).trim();
            String innerMessage = extractInnerBodyMessage(bodyJson);

            if (StringUtils.hasText(outerMessage) && StringUtils.hasText(innerMessage)) {
                return outerMessage + " " + innerMessage;
            }
            if (StringUtils.hasText(outerMessage)) {
                return outerMessage;
            }
            if (StringUtils.hasText(innerMessage)) {
                return innerMessage;
            }
            return message;
        } catch (Exception ex) {
            log.warn("Pay 승인 실패 응답 파싱 실패. 원문을 저장합니다. responseBody={}", responseBody, ex);
            return responseBody;
        }
    }

    private String extractInnerBodyMessage(String bodyJson) {
        if (!StringUtils.hasText(bodyJson)) {
            return null;
        }

        try {
            JsonNode bodyNode = OBJECT_MAPPER.readTree(bodyJson);
            String bodyMessage = bodyNode.path("message").asText(null);
            return StringUtils.hasText(bodyMessage) ? bodyMessage : null;
        } catch (Exception ex) {
            log.warn("Pay 승인 실패 body 파싱 실패. bodyJson={}", bodyJson, ex);
            return null;
        }
    }
}
