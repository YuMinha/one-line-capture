CREATE TABLE link (
    capture_id  BIGINT        NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    note        VARCHAR(300)  NULL,                 -- URL 앞뒤에 붙은 설명 텍스트
    read_at     TIMESTAMP     NULL,                 -- NULL이면 안 읽음
    PRIMARY KEY (capture_id),
    CONSTRAINT fk_link_capture FOREIGN KEY (capture_id)
        REFERENCES capture(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
