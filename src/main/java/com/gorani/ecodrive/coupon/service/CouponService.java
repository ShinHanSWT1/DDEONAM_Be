package com.gorani.ecodrive.coupon.service;

import com.gorani.ecodrive.coupon.domain.CouponCheckoutAttempt;
import com.gorani.ecodrive.coupon.domain.CouponTemplate;
import com.gorani.ecodrive.coupon.domain.CouponUseToken;
import com.gorani.ecodrive.coupon.domain.UserCoupon;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutConfirmRequest;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutConfirmResponse;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutPrepareRequest;
import com.gorani.ecodrive.coupon.dto.CouponCheckoutPrepareResponse;
import com.gorani.ecodrive.coupon.dto.CouponPurchaseRequest;
import com.gorani.ecodrive.coupon.dto.CouponPurchaseResponse;
import com.gorani.ecodrive.coupon.dto.CouponTemplateResponse;
import com.gorani.ecodrive.coupon.dto.CouponUseTokenResponse;
import com.gorani.ecodrive.coupon.dto.UserCouponResponse;
import com.gorani.ecodrive.coupon.repository.CouponCheckoutAttemptRepository;
import com.gorani.ecodrive.coupon.repository.CouponTemplateRepository;
import com.gorani.ecodrive.coupon.repository.CouponUseTokenRepository;
import com.gorani.ecodrive.coupon.repository.UserCouponRepository;
import com.gorani.ecodrive.pay.dto.PayCheckoutRequest;
import com.gorani.ecodrive.pay.dto.PayCheckoutResponse;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private static final String ACTIVE = "ACTIVE";

    private final CouponTemplateRepository couponTemplateRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponUseTokenRepository couponUseTokenRepository;
    private final CouponCheckoutAttemptRepository couponCheckoutAttemptRepository;
    private final PayIntegrationService payIntegrationService;

    public List<CouponTemplateResponse> getActiveTemplates() {
        return couponTemplateRepository.findByStatusOrderByIdAsc(ACTIVE).stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    public CouponTemplateResponse getActiveTemplate(Long templateId) {
        CouponTemplate template = couponTemplateRepository.findByIdAndStatus(templateId, ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
        return toTemplateResponse(template);
    }

    public List<UserCouponResponse> getUserCoupons(Long userId) {
        return userCouponRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(this::toUserCouponResponse)
                .toList();
    }

    /**
     * 하위 호환용 즉시 구매 API
     * 신규 플로우는 prepare/confirm 2단계를 사용한다.
     */
    @Transactional
    public CouponPurchaseResponse purchaseCoupon(Long userId, CouponPurchaseRequest request) {
        CouponTemplate template = getActiveTemplateEntity(request.couponTemplateId());
        int amount = resolveCouponPrice(template);
        int pointAmount = resolvePointAmount(request.pointAmount(), amount);

        log.info("쿠폰 즉시 결제 시작(구버전 경로). userId={}, couponTemplateId={}, amount={}, pointAmount={}",
                userId, request.couponTemplateId(), amount, pointAmount);

        PayCheckoutResponse payment = payIntegrationService.checkout(userId, new PayCheckoutRequest(
                template.getName(),
                amount,
                "WALLET",
                pointAmount,
                0,
                template.getId()
        ));

        if (!"COMPLETED".equalsIgnoreCase(payment.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "쿠폰 결제가 완료되지 않았습니다.");
        }

        UserCoupon issued = userCouponRepository.save(UserCoupon.issue(userId, template, LocalDateTime.now()));
        log.info("쿠폰 즉시 결제/발급 완료. userId={}, couponTemplateId={}, userCouponId={}, paymentId={}",
                userId, template.getId(), issued.getId(), payment.paymentId());

        return new CouponPurchaseResponse(toUserCouponResponse(issued), payment);
    }

    /**
     * FE -> BE -> PAY 흐름의 1단계: 결제 준비 + PAY 결제창 URL 발급
     */
    @Transactional
    public CouponCheckoutPrepareResponse prepareCheckout(Long userId, CouponCheckoutPrepareRequest request) {
        CouponTemplate template = getActiveTemplateEntity(request.couponTemplateId());
        int amount = resolveCouponPrice(template);
        int pointAmount = resolvePointAmount(request.pointAmount(), amount);
        int finalAmount = amount - pointAmount;

        String orderId = buildCouponOrderId(userId);
        PayCheckoutSessionResponse session = payIntegrationService.createCheckoutSession(
                userId,
                new PayCheckoutSessionRequest(
                        orderId,
                        template.getName(),
                        amount,
                        pointAmount,
                        0,
                        template.getId(),
                        request.successUrl(),
                        request.failUrl(),
                        "MERCHANT_REDIRECT",
                        "REDIRECT"
                )
        );

        LocalDateTime now = LocalDateTime.now();
        CouponCheckoutAttempt attempt = couponCheckoutAttemptRepository.save(
                CouponCheckoutAttempt.prepare(
                        userId,
                        template.getId(),
                        orderId,
                        session.sessionToken(),
                        template.getName(),
                        amount,
                        pointAmount,
                        0,
                        finalAmount,
                        session.expiresAt(),
                        now
                )
        );

        log.info("쿠폰 checkout 준비 완료. userId={}, attemptId={}, orderId={}, sessionToken={}, amount={}, pointAmount={}, finalAmount={}",
                userId, attempt.getId(), orderId, session.sessionToken(), amount, pointAmount, finalAmount);

        return new CouponCheckoutPrepareResponse(
                orderId,
                session.sessionToken(),
                session.checkoutUrl(),
                amount,
                pointAmount,
                finalAmount,
                session.expiresAt()
        );
    }

    /**
     * FE -> BE -> PAY 흐름의 2단계: PAY 성공 리다이렉트 이후 쿠폰 발급 확정
     */
    @Transactional
    public CouponCheckoutConfirmResponse confirmCheckout(Long userId, CouponCheckoutConfirmRequest request) {
        CouponCheckoutAttempt attempt = couponCheckoutAttemptRepository.findByUserIdAndOrderId(userId, request.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "결제 준비 정보를 찾을 수 없습니다."));

        if ("COMPLETED".equalsIgnoreCase(attempt.getStatus())) {
            if (attempt.getIssuedCouponId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 완료된 결제이지만 쿠폰 정보가 누락되었습니다.");
            }
            UserCoupon issued = userCouponRepository.findById(attempt.getIssuedCouponId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "발급된 쿠폰 정보를 찾을 수 없습니다."));
            return new CouponCheckoutConfirmResponse(attempt.getOrderId(), attempt.getPaymentId(), toUserCouponResponse(issued));
        }

        if (!"PREPARED".equalsIgnoreCase(attempt.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "확정 가능한 결제 상태가 아닙니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (attempt.isExpired(now)) {
            attempt.markFailed("결제 유효시간이 만료되었습니다.", now);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 유효시간이 만료되었습니다.");
        }

        // PAY 성공 리다이렉트 amount는 연동 버전에 따라 원금/최종결제금(포인트 차감 후)으로 올 수 있다.
        int expectedAmount = attempt.getAmount();
        int expectedFinalAmount = attempt.getFinalAmount();
        boolean matchesAmount = request.amount() == expectedAmount || request.amount() == expectedFinalAmount;
        if (!matchesAmount) {
            log.warn("쿠폰 결제 금액 검증 실패. userId={}, orderId={}, requestAmount={}, expectedAmount={}, expectedFinalAmount={}",
                    userId, request.orderId(), request.amount(), expectedAmount, expectedFinalAmount);
            attempt.markFailed("결제 금액 검증 실패", now);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다.");
        }

        if (request.status() != null && !"COMPLETED".equalsIgnoreCase(request.status())) {
            attempt.markFailed("PAY 결제 상태가 COMPLETED가 아님", now);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제가 완료되지 않았습니다.");
        }

        CouponTemplate template = getActiveTemplateEntity(attempt.getCouponTemplateId());
        UserCoupon issued = userCouponRepository.save(UserCoupon.issue(userId, template, now));
        attempt.markCompleted(request.paymentId(), issued.getId(), now);

        log.info("쿠폰 checkout 확정 완료. userId={}, orderId={}, paymentId={}, issuedCouponId={}",
                userId, attempt.getOrderId(), request.paymentId(), issued.getId());

        return new CouponCheckoutConfirmResponse(attempt.getOrderId(), request.paymentId(), toUserCouponResponse(issued));
    }

    @Transactional
    public CouponUseTokenResponse issueUseToken(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdAndUserId(userCouponId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "보유 쿠폰을 찾을 수 없습니다."));

        if (!"AVAILABLE".equalsIgnoreCase(userCoupon.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용 가능한 쿠폰 상태가 아닙니다.");
        }
        if (userCoupon.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "만료된 쿠폰입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 동일 쿠폰의 기존 발급 토큰은 재사용 방지를 위해 만료 처리한다.
        List<CouponUseToken> issuedTokens = couponUseTokenRepository.findByUserCouponIdAndStatus(userCouponId, "ISSUED");
        issuedTokens.forEach(token -> token.expire(now));

        String oneTimeCode = buildOneTimeCode();
        String qrPayload = "GORANI-COUPON:" + oneTimeCode;
        LocalDateTime expiresAt = now.plusMinutes(5);

        CouponUseToken saved = couponUseTokenRepository.save(
                CouponUseToken.issue(userCoupon, oneTimeCode, qrPayload, now, expiresAt)
        );

        log.info("쿠폰 사용 토큰 발급 완료. userId={}, userCouponId={}, tokenId={}, expiresAt={}",
                userId, userCouponId, saved.getId(), expiresAt);

        return new CouponUseTokenResponse(userCouponId, oneTimeCode, qrPayload, expiresAt);
    }

    private CouponTemplate getActiveTemplateEntity(Long templateId) {
        return couponTemplateRepository.findByIdAndStatus(templateId, ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰 템플릿을 찾을 수 없습니다."));
    }

    private int resolveCouponPrice(CouponTemplate template) {
        String discount = template.getDiscountLabel();
        if (discount == null) {
            return 3000;
        }
        String onlyDigits = discount.replaceAll("[^0-9]", "");
        if (onlyDigits.isBlank()) {
            return 3000;
        }
        return Integer.parseInt(onlyDigits);
    }

    private int resolvePointAmount(Integer pointAmount, int amount) {
        int resolved = pointAmount == null ? 0 : pointAmount;
        if (resolved < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트 사용 금액은 0 이상이어야 합니다.");
        }
        if (resolved > amount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트 사용 금액이 결제 금액을 초과할 수 없습니다.");
        }
        return resolved;
    }

    private String buildCouponOrderId(Long userId) {
        return "ECO-COUPON-" + userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String buildOneTimeCode() {
        return "CPN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private CouponTemplateResponse toTemplateResponse(CouponTemplate template) {
        return new CouponTemplateResponse(
                template.getId(),
                template.getCategory(),
                template.getName(),
                template.getDiscountLabel(),
                template.getValidDays()
        );
    }

    private UserCouponResponse toUserCouponResponse(UserCoupon userCoupon) {
        CouponTemplate template = userCoupon.getCouponTemplate();
        return new UserCouponResponse(
                userCoupon.getId(),
                template.getId(),
                template.getName(),
                template.getCategory(),
                template.getDiscountLabel(),
                userCoupon.getStatus(),
                userCoupon.getIssuedAt(),
                userCoupon.getExpiresAt()
        );
    }
}
