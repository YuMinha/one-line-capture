CREATE TABLE expense (
    capture_id  BIGINT        NOT NULL,
    amount      DECIMAL(12,2) NOT NULL,             -- 절대 DOUBLE 쓰지 말 것
    merchant    VARCHAR(100)  NULL,                 -- "점심", "스벅"
    spent_at    DATE          NOT NULL,             -- 지출일 (v1은 입력일과 동일)
    PRIMARY KEY (capture_id),
    KEY idx_expense_spent_at (spent_at),
    CONSTRAINT fk_expense_capture FOREIGN KEY (capture_id)
        REFERENCES capture(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
