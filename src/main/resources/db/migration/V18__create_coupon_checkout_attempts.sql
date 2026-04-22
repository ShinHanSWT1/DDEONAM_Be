CREATE TABLE coupon_checkout_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_template_id BIGINT NOT NULL,
    order_id VARCHAR(80) NOT NULL UNIQUE,
    session_token VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    amount INTEGER NOT NULL,
    point_amount INTEGER NOT NULL DEFAULT 0,
    coupon_discount_amount INTEGER NOT NULL DEFAULT 0,
    final_amount INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_id BIGINT,
    issued_coupon_id BIGINT,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_coupon_checkout_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_coupon_checkout_attempts_template FOREIGN KEY (coupon_template_id) REFERENCES coupon_templates(id),
    CONSTRAINT fk_coupon_checkout_attempts_issued_coupon FOREIGN KEY (issued_coupon_id) REFERENCES user_coupons(id)
);

CREATE INDEX idx_coupon_checkout_attempts_user_status ON coupon_checkout_attempts(user_id, status);
CREATE INDEX idx_coupon_checkout_attempts_order ON coupon_checkout_attempts(order_id);
