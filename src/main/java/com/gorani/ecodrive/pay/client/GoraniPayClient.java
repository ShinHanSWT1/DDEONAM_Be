package com.gorani.ecodrive.pay.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class GoraniPayClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RestTemplate restTemplate;
    private final String internalToken;

    public GoraniPayClient(
            @Value("${app.pay.base-url:http://localhost:8083/pay}") String baseUrl,
            @Value("${app.pay.internal-token:}") String internalToken
    ) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUrl));
        this.internalToken = internalToken;
    }

    public PayAccountPayload createAccount(CreateAccountPayload request) {
        log.info("gorani_pay 계좌 생성 호출. externalUserId={}", request.externalUserId());
        PayAccountPayload response = restTemplate.postForObject(
                "/accounts",
                new HttpEntity<>(request, headers()),
                PayAccountPayload.class
        );
        if (response == null) {
            throw new IllegalStateException("gorani_pay account create response is empty");
        }
        return response;
    }

    public PayAccountPayload getAccountByExternalUserId(Long externalUserId) {
        log.info("gorani_pay 외부 사용자 기반 계좌 조회 호출. externalUserId={}", externalUserId);
        ResponseEntity<PayAccountPayload> response = restTemplate.exchange(
                "/accounts/by-external-user/{externalUserId}",
                HttpMethod.GET,
                new HttpEntity<>(null, headers()),
                PayAccountPayload.class,
                externalUserId
        );
        if (!response.hasBody() || response.getBody() == null) {
            throw new IllegalStateException("gorani_pay account lookup by external user response is empty");
        }
        return response.getBody();
    }

    public PayAccountPayload getAccount(Long payUserId) {
        log.info("gorani_pay 계좌 조회 호출. payUserId={}", payUserId);
        ResponseEntity<PayAccountPayload> response = restTemplate.exchange(
                "/account/{payUserId}",
                HttpMethod.GET,
                new HttpEntity<>(null, headers()),
                PayAccountPayload.class,
                payUserId
        );
        if (!response.hasBody() || response.getBody() == null) {
            throw new IllegalStateException("gorani_pay account lookup response is empty");
        }
        return response.getBody();
    }

    public List<PayTransactionPayload> getTransactions(Long payUserId) {
        log.info("gorani_pay 거래내역 조회 호출. payUserId={}", payUserId);
        ResponseEntity<PayTransactionPayload[]> response = restTemplate.exchange(
                "/account/{payUserId}/transactions",
                HttpMethod.GET,
                new HttpEntity<>(null, headers()),
                PayTransactionPayload[].class,
                payUserId
        );
        PayTransactionPayload[] body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("gorani_pay transactions lookup response is empty");
        }
        return List.of(body);
    }

    public PayAccountPayload charge(Long payUserId, Integer amount) {
        log.info("gorani_pay 충전 호출. payUserId={}, amount={}", payUserId, amount);
        PayAccountPayload response = restTemplate.postForObject(
                "/charge",
                new HttpEntity<>(new AmountPayload(payUserId, amount), headers()),
                PayAccountPayload.class
        );
        if (response == null) {
            throw new IllegalStateException("gorani_pay charge response is empty");
        }
        return response;
    }

    public PayAccountPayload confirmCharge(Long payUserId, String paymentKey, String orderId, Integer amount) {
        log.info("gorani_pay 토스 충전 승인 호출. payUserId={}, orderId={}, amount={}", payUserId, orderId, amount);
        PayAccountPayload response = restTemplate.postForObject(
                "/charge/confirm",
                new HttpEntity<>(new ChargeConfirmPayload(payUserId, paymentKey, orderId, amount), headers()),
                PayAccountPayload.class
        );
        if (response == null) {
            throw new IllegalStateException("gorani_pay charge confirm response is empty");
        }
        return response;
    }

    public PayPaymentPayload createPayment(CreatePaymentPayload request, String idempotencyKey) {
        log.info("gorani_pay 결제 생성 호출. payUserId={}, payAccountId={}, amount={}, externalOrderId={}",
                request.payUserId(), request.payAccountId(), request.amount(), request.externalOrderId());
        HttpHeaders headers = headers();
        headers.set(IDEMPOTENCY_HEADER, idempotencyKey);
        PayPaymentPayload response = restTemplate.postForObject(
                "/payments",
                new HttpEntity<>(request, headers),
                PayPaymentPayload.class
        );
        if (response == null) {
            throw new IllegalStateException("gorani_pay create payment response is empty");
        }
        return response;
    }

    public PayPaymentPayload completePayment(Long paymentId, String idempotencyKey) {
        log.info("gorani_pay 결제 확정 호출. paymentId={}", paymentId);
        HttpHeaders headers = headers();
        headers.set(IDEMPOTENCY_HEADER, idempotencyKey);
        PayPaymentPayload response = restTemplate.postForObject(
                "/payments/{paymentId}/complete",
                new HttpEntity<>(null, headers),
                PayPaymentPayload.class,
                paymentId
        );
        if (response == null) {
            throw new IllegalStateException("gorani_pay complete payment response is empty");
        }
        return response;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtils.hasText(internalToken)) {
            headers.set(INTERNAL_TOKEN_HEADER, internalToken);
        }
        return headers;
    }

    public record CreateAccountPayload(
            Long externalUserId,
            String userName,
            String email,
            String ownerName,
            String bankCode,
            String accountNumber
    ) {
    }

    private record AmountPayload(
            Long payUserId,
            Integer amount
    ) {
    }

    private record ChargeConfirmPayload(
            Long payUserId,
            String paymentKey,
            String orderId,
            Integer amount
    ) {
    }

    public record CreatePaymentPayload(
            Long payUserId,
            Long payAccountId,
            String externalOrderId,
            String paymentType,
            Long payProductId,
            String title,
            Integer amount,
            Integer pointAmount,
            Integer couponDiscountAmount
    ) {
    }

    public record PayAccountPayload(
            Long id,
            Long payUserId,
            String accountNumber,
            String bankCode,
            String ownerName,
            Integer balance,
            String status
    ) {
    }

    public record PayPaymentPayload(
            Long id,
            Long payUserId,
            Long payAccountId,
            String externalOrderId,
            String paymentType,
            Long payProductId,
            String title,
            Integer amount,
            Integer pointAmount,
            Integer couponDiscountAmount,
            String status
    ) {
    }

    public record PayTransactionPayload(
            Long id,
            Long payAccountId,
            Long payPaymentId,
            String transactionType,
            String direction,
            Integer amount,
            String category,
            LocalDateTime occurredAt
    ) {
    }
}
