CREATE TABLE user_email_provider
(
    user_id     BIGINT NOT NULL,
    provider_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, provider_id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (provider_id) REFERENCES email_provider (id)
);