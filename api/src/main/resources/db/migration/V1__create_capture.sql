CREATE TABLE capture (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    raw_text    VARCHAR(500) NOT NULL,              -- 사용자가 던진 원문. 절대 지우지 않는다
    type        VARCHAR(20)  NOT NULL,              -- EXPENSE | TODO | LINK
    source      VARCHAR(20)  NOT NULL,              -- AUTO | MANUAL (사용자가 고쳤는지)
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_capture_type_created (type, created_at DESC),
    KEY idx_capture_created (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
