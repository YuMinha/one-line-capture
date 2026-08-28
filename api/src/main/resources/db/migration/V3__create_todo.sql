CREATE TABLE todo (
    capture_id  BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    due_at      TIMESTAMP    NULL,                  -- 파싱 실패 시 NULL
    done        BOOLEAN      NOT NULL DEFAULT FALSE,
    done_at     TIMESTAMP    NULL,
    PRIMARY KEY (capture_id),
    KEY idx_todo_done_due (done, due_at),
    CONSTRAINT fk_todo_capture FOREIGN KEY (capture_id)
        REFERENCES capture(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
