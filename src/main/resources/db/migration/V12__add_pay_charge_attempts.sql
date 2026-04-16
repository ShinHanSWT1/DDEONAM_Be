CREATE TABLE pay_charge_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(80) NOT NULL,
    payment_key VARCHAR(200),
    amount INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    error_code VARCHAR(50),
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_pay_charge_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_pay_charge_attempts_order_id UNIQUE (order_id),
    CONSTRAINT uk_pay_charge_attempts_payment_key UNIQUE (payment_key)
);

CREATE INDEX idx_pay_charge_attempts_user ON pay_charge_attempts(user_id);
CREATE INDEX idx_pay_charge_attempts_status ON pay_charge_attempts(status);
