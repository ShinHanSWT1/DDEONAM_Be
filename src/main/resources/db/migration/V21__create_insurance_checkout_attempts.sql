CREATE TABLE insurance_checkout_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    insurance_contract_id BIGINT NOT NULL,
    user_vehicle_id BIGINT NOT NULL,
    insurance_product_id BIGINT NOT NULL,
    order_id VARCHAR(80) NOT NULL UNIQUE,
    session_token VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    amount INTEGER NOT NULL,
    point_amount INTEGER NOT NULL DEFAULT 0,
    coupon_discount_amount INTEGER NOT NULL DEFAULT 0,
    final_amount INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_id BIGINT,
    user_insurance_id BIGINT,
    selected_coverage_ids VARCHAR(1000),
    signature_image TEXT,
    email VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_insurance_checkout_attempts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_insurance_checkout_attempts_contract FOREIGN KEY (insurance_contract_id) REFERENCES insurance_contracts(id),
    CONSTRAINT fk_insurance_checkout_attempts_product FOREIGN KEY (insurance_product_id) REFERENCES insurance_products(id),
    CONSTRAINT fk_insurance_checkout_attempts_user_insurance FOREIGN KEY (user_insurance_id) REFERENCES user_insurances(id)
);

CREATE INDEX idx_insurance_checkout_attempts_user_status ON insurance_checkout_attempts(user_id, status);
CREATE INDEX idx_insurance_checkout_attempts_order ON insurance_checkout_attempts(order_id);
