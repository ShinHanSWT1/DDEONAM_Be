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

WITH ranked_user_insurances AS (
    SELECT
        id,
        row_number() OVER (
            PARTITION BY user_vehicle_id
            ORDER BY created_at DESC NULLS LAST, id DESC
        ) AS row_num
    FROM user_insurances
)
UPDATE user_insurances ui
SET status = CASE
                 WHEN ranked.row_num = 1 THEN 'ACTIVE'
                 ELSE 'INACTIVE'
             END,
    ended_at = CASE
                   WHEN ranked.row_num = 1 THEN ui.ended_at
                   ELSE COALESCE(ui.ended_at, CURRENT_TIMESTAMP)
               END
FROM ranked_user_insurances ranked
WHERE ui.id = ranked.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_insurances_active_vehicle
    ON user_insurances (user_vehicle_id)
    WHERE status = 'ACTIVE';
