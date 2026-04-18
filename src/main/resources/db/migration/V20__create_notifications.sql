CREATE TABLE notifications
(
    id         BIGSERIAL    NOT NULL,
    user_id    BIGINT       NOT NULL,
    type       VARCHAR(30)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    body       VARCHAR(255) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
