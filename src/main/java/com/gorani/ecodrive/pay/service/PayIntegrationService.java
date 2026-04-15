package com.gorani.ecodrive.pay.service;

import com.gorani.ecodrive.common.exception.CustomException;
import com.gorani.ecodrive.common.exception.ErrorCode;
import com.gorani.ecodrive.pay.client.GoraniPayClient;
import com.gorani.ecodrive.pay.dto.PayChargeRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutResponse;
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

    private final GoraniPayClient goraniPayClient;
    private final UserService userService;

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
    public PayWalletResponse charge(Long userId, PayChargeRequest request) {
        log.info("Pay 충전 시작. userId={}, amount={}", userId, request.amount());
        PayWalletResponse wallet = getWallet(userId);
        GoraniPayClient.PayAccountPayload charged = goraniPayClient.charge(wallet.payUserId(), request.amount());
        log.info("Pay 충전 완료. userId={}, payUserId={}, payAccountId={}, balance={}",
                userId, charged.payUserId(), charged.id(), charged.balance());
        return toWalletResponse(charged);
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
                ? payload.category()
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
            case "CHARGE" -> "잔액 충전";
            case "WITHDRAW" -> "출금";
            case "PAYMENT" -> "결제";
            case "REFUND" -> "환불";
            default -> transactionType;
        };
    }
}
