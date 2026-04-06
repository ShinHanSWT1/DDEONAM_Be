CREATE TABLE users
(
    id                      BIGSERIAL PRIMARY KEY,
    email                   VARCHAR(100) NOT NULL UNIQUE,
    nickname                VARCHAR(50)  NOT NULL,
    profile_image_url       VARCHAR(255),
    role                    VARCHAR(20)  NOT NULL DEFAULT 'USER',
    provider                VARCHAR(20)  NOT NULL,
    provider_user_id        VARCHAR(100) NOT NULL,
    is_onboarding_completed BOOLEAN      NOT NULL DEFAULT FALSE,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    age                     INTEGER,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP    NOT NULL
);

CREATE TABLE vehicle_models
(
    id                    BIGSERIAL PRIMARY KEY,
    manufacturer          VARCHAR(50)  NOT NULL,
    model_name            VARCHAR(100) NOT NULL,
    model_year            SMALLINT     NOT NULL,
    fuel_type             VARCHAR(20)  NOT NULL,
    body_type             VARCHAR(30),
    base_insurance_factor NUMERIC(8, 4),
    created_at            TIMESTAMP    NOT NULL
);

CREATE TABLE user_vehicles
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    vehicle_model_id BIGINT      NOT NULL,
    vehicle_number   VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    registered_at    TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP   NOT NULL,

    CONSTRAINT fk_user_vehicle_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_vehicle_model FOREIGN KEY (vehicle_model_id) REFERENCES vehicle_models (id)
);

CREATE TABLE payment_links
(
    user_id                 BIGINT PRIMARY KEY,
    pay_user_external_id    VARCHAR(50) NOT NULL,
    pay_account_external_id VARCHAR(50),
    link_status             VARCHAR(20) NOT NULL DEFAULT 'UNLINKED',
    linked_at               TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE payment_references
(
    id                          BIGSERIAL PRIMARY KEY,
    user_id                     BIGINT      NOT NULL,
    payment_type                VARCHAR(20) NOT NULL, -- PAY / POINT / COUPON
    source_type                 VARCHAR(30) NOT NULL, -- MISSION / INSURANCE / SHOP
    source_id                   BIGINT,
    pay_payment_external_id     VARCHAR(50) NOT NULL,
    pay_transaction_external_id VARCHAR(50),
    amount                      INTEGER     NOT NULL,
    point_amount                INTEGER     NOT NULL,

    status                      VARCHAR(20) NOT NULL DEFAULT 'READY',
    title                       VARCHAR(100),
    created_at                  TIMESTAMP   NOT NULL,
    updated_at                  TIMESTAMP   NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE auth_refresh_tokens
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    refresh_token VARCHAR(512) NOT NULL,
    expires_at    TIMESTAMP    NOT NULL,
    revoked_at    TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_notification_settings
(
    user_id           BIGINT PRIMARY KEY,
    push_enabled      BOOLEAN   NOT NULL,
    mission_enabled   BOOLEAN   NOT NULL,
    insurance_enabled BOOLEAN   NOT NULL,
    reward_enabled    BOOLEAN   NOT NULL,
    updated_at        TIMESTAMP NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE admin_audit_logs
(
    id            BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT      NOT NULL,
    target_type   VARCHAR(30) NOT NULL,
    target_id     BIGINT,
    action        VARCHAR(50) NOT NULL,
    before_data   JSONB,
    after_data    JSONB,
    created_at    TIMESTAMP   NOT NULL
);

CREATE TABLE driving_sessions
(
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT        NOT NULL,
    user_vehicle_id      BIGINT        NOT NULL,
    session_date         DATE          NOT NULL,
    started_at           TIMESTAMP     NOT NULL,
    ended_at             TIMESTAMP     NOT NULL,
    distance_km          NUMERIC(8, 2) NOT NULL,
    driving_time_minutes INTEGER       NOT NULL,
    idling_time_minutes  INTEGER,
    average_speed        NUMERIC(5, 2),
    max_speed            NUMERIC(5, 2),
    created_at           TIMESTAMP     NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (user_vehicle_id) REFERENCES user_vehicles (id)
);

CREATE TABLE driving_events
(
    id                 BIGSERIAL PRIMARY KEY,
    driving_session_id BIGINT      NOT NULL,
    user_id            BIGINT      NOT NULL,
    event_type         VARCHAR(30) NOT NULL,
    event_value        NUMERIC(8, 2),
    occurred_at        TIMESTAMP   NOT NULL,
    score_delta        INTEGER,
    created_at         TIMESTAMP   NOT NULL,

    FOREIGN KEY (driving_session_id) REFERENCES driving_sessions (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE driving_score_snapshots
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT    NOT NULL,
    snapshot_date DATE      NOT NULL,
    score         INTEGER   NOT NULL,
    created_at    TIMESTAMP NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE driving_score_change_logs
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    snapshot_id   BIGINT      NOT NULL,
    change_date   DATE        NOT NULL,
    change_type   VARCHAR(30) NOT NULL,
    message       VARCHAR(255),
    score_delta   INTEGER,
    display_order INTEGER,
    created_at    TIMESTAMP   NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (snapshot_id) REFERENCES driving_score_snapshots (id)
);

CREATE TABLE carbon_reduction_snapshots
(
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    snapshot_date       DATE          NOT NULL,
    carbon_reduction_kg NUMERIC(8, 2) NOT NULL,
    reward_point        INTEGER       NOT NULL,
    created_at          TIMESTAMP     NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE insurance_companies
(
    id           BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(100) NOT NULL,
    code         VARCHAR(30)  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP    NOT NULL
);

CREATE TABLE insurance_products
(
    id                   BIGSERIAL PRIMARY KEY,
    insurance_company_id BIGINT       NOT NULL,
    product_name         VARCHAR(100) NOT NULL,
    base_amount          INTEGER,
    discount_rate        NUMERIC(5, 2),
    status               VARCHAR(20) DEFAULT 'ACTIVE',
    created_at           TIMESTAMP    NOT NULL,

    FOREIGN KEY (insurance_company_id) REFERENCES insurance_companies (id)
);

CREATE TABLE insurance_contracts
(
    id                         BIGSERIAL PRIMARY KEY,
    user_id                    BIGINT NOT NULL,
    insurance_product_id       BIGINT NOT NULL,
    driving_score_snapshots_id BIGINT NOT NULL,

    phone_number               VARCHAR(20),
    address                    VARCHAR(255),
    contract_period            INTEGER,
    plan_type                  VARCHAR(20),
    status                     VARCHAR(20),

    started_at                 TIMESTAMP,
    ended_at                   TIMESTAMP,
    created_at                 TIMESTAMP,

    base_amount                INTEGER,
    discount_amount            INTEGER,
    discount_rate              NUMERIC(5, 2),
    final_amount               INTEGER,

    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (insurance_product_id) REFERENCES insurance_products (id),
    FOREIGN KEY (driving_score_snapshots_id) REFERENCES driving_score_snapshots (id)
);

CREATE TABLE user_insurances
(
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT    NOT NULL,
    user_vehicle_id        BIGINT    NOT NULL,
    insurance_company_id   BIGINT    NOT NULL,
    insurance_product_id   BIGINT,
    insurance_contracts_id BIGINT    NOT NULL,
    created_at             TIMESTAMP NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (user_vehicle_id) REFERENCES user_vehicles (id),
    FOREIGN KEY (insurance_company_id) REFERENCES insurance_companies (id),
    FOREIGN KEY (insurance_product_id) REFERENCES insurance_products (id),
    FOREIGN KEY (insurance_contracts_id) REFERENCES insurance_contracts (id)
);

CREATE TABLE insurance_coverages
(
    id                    BIGSERIAL PRIMARY KEY,
    insurance_products_id BIGINT NOT NULL,

    category              VARCHAR(50),
    coverage_name         VARCHAR(100),
    coverage_amount       INTEGER,
    is_required           BOOLEAN,
    plan_type             VARCHAR(20),
    status                VARCHAR(20),

    created_at            TIMESTAMP,

    FOREIGN KEY (insurance_products_id) REFERENCES insurance_products (id)
);

CREATE TABLE contract_coverages
(
    id                     BIGSERIAL PRIMARY KEY,

    insurance_contracts_id BIGINT NOT NULL,
    insurance_coverages_id BIGINT NOT NULL,
    insurance_products_id  BIGINT NOT NULL,

    category               VARCHAR(50),
    coverage_amount        INTEGER,

    created_at             TIMESTAMP,

    FOREIGN KEY (insurance_contracts_id) REFERENCES insurance_contracts (id),
    FOREIGN KEY (insurance_coverages_id) REFERENCES insurance_coverages (id),
    FOREIGN KEY (insurance_products_id) REFERENCES insurance_products (id)
);

CREATE TABLE coupon_templates
(
    id             BIGSERIAL PRIMARY KEY,
    category       VARCHAR(30)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    discount_label VARCHAR(50)  NOT NULL,
    valid_days     INTEGER      NOT NULL,
    is_pay_usable  BOOLEAN      NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP    NOT NULL
);

CREATE TABLE user_coupons
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT      NOT NULL,
    coupon_template_id BIGINT      NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    issued_at          TIMESTAMP   NOT NULL,
    expires_at         TIMESTAMP   NOT NULL,
    used_at            TIMESTAMP,
    used_payment_id    BIGINT,
    created_at         TIMESTAMP   NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (coupon_template_id) REFERENCES coupon_templates (id),
    FOREIGN KEY (used_payment_id) REFERENCES payment_references (id)
);

CREATE TABLE point_wallets
(
    user_id               BIGINT PRIMARY KEY,
    point_balance         INTEGER   NOT NULL,
    lifetime_earned_point INTEGER   NOT NULL,
    lifetime_used_point   INTEGER   NOT NULL,
    updated_at            TIMESTAMP NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE point_transactions
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL,
    transaction_type VARCHAR(20) NOT NULL, -- EARN / USE / EXPIRE
    source_type      VARCHAR(30) NOT NULL, -- MISSION / PAYMENT / ADMIN
    source_id        BIGINT,
    amount           INTEGER     NOT NULL,
    balance_after    INTEGER     NOT NULL,
    description      VARCHAR(255),
    created_at       TIMESTAMP   NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE mission_policies
(
    id           BIGSERIAL PRIMARY KEY,
    mission_type VARCHAR(20)    NOT NULL,
    category     VARCHAR(30)    NOT NULL,
    title        VARCHAR(100)   NOT NULL,
    description  VARCHAR(255),
    target_type  VARCHAR(30)    NOT NULL,
    target_value NUMERIC(10, 2) NOT NULL,
    reward_point INTEGER        NOT NULL,

    status       VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',

    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL
);

CREATE TABLE user_missions
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT         NOT NULL,
    mission_policy_id BIGINT         NOT NULL,
    period_start_date DATE           NOT NULL,
    period_end_date   DATE           NOT NULL,
    current_value     NUMERIC(10, 2) NOT NULL,
    progress_rate     NUMERIC(5, 2)  NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'IN_PROGRESS',
    rewarded_at       TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (mission_policy_id) REFERENCES mission_policies (id)
);