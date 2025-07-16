CREATE TABLE koreader_user
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    username         VARCHAR(100) NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    display_name     VARCHAR(255),
    booklore_user_id BIGINT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booklore_user FOREIGN KEY (booklore_user_id) REFERENCES users (id)
);