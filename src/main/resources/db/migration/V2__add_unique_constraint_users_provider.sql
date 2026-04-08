ALTER TABLE users
    ADD CONSTRAINT uq_users_provider_provider_user_id
        UNIQUE (provider, provider_user_id);
