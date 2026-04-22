CREATE TABLE point_reward_grants (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reward_type VARCHAR(20) NOT NULL,
    reward_ref VARCHAR(120) NOT NULL,
    point_amount INTEGER NOT NULL,
    period_start_date DATE,
    period_end_date DATE,
    source_ref VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_point_reward_grants_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_point_reward_grants_reward_ref UNIQUE (reward_ref)
);

CREATE INDEX idx_point_reward_grants_user_created_at
    ON point_reward_grants (user_id, created_at DESC);

