ALTER TABLE user_insurances
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE user_insurances
    ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_user_insurances_vehicle_contract'
    ) THEN
        ALTER TABLE user_insurances
            ADD CONSTRAINT uq_user_insurances_vehicle_contract
                UNIQUE (user_vehicle_id, insurance_contracts_id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_insurances_active_vehicle
    ON user_insurances (user_vehicle_id)
    WHERE status = 'ACTIVE';
