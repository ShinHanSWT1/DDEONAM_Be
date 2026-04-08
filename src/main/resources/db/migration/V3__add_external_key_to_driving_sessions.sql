ALTER TABLE driving_sessions
    ADD COLUMN external_key VARCHAR(100);

CREATE UNIQUE INDEX uq_driving_sessions_external_key
    ON driving_sessions (external_key)
    WHERE external_key IS NOT NULL;
