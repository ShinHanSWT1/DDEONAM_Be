CREATE TABLE coupon_use_tokens
(
    id             BIGSERIAL PRIMARY KEY,
    user_coupon_id BIGINT      NOT NULL,
    one_time_code  VARCHAR(64) NOT NULL UNIQUE,
    qr_payload     VARCHAR(255) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    expires_at     TIMESTAMP   NOT NULL,
    used_at        TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL,

    CONSTRAINT fk_coupon_use_tokens_user_coupon
        FOREIGN KEY (user_coupon_id) REFERENCES user_coupons (id)
);

CREATE INDEX idx_coupon_use_tokens_user_coupon_status
    ON coupon_use_tokens (user_coupon_id, status);
