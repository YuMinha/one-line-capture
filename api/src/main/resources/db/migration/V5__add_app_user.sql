-- 게스트 모드 (stack.md §2.1, §5). 토큰 원문은 저장하지 않는다. DB가 새도 토큰은 안 나간다
CREATE TABLE app_user (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    token_hash  VARCHAR(64) NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주인. 해시는 앱 기동 시 OwnerTokenSync가 API_TOKEN으로 채운다. SQL은 환경변수를 모른다
INSERT INTO app_user (id) VALUES (1);

-- 기존 행은 전부 주인 것이다. DEFAULT로 채운 뒤 떼서, 앞으로는 소유자를 빠뜨리면 INSERT가 실패하게 한다
ALTER TABLE capture ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1 AFTER id;
ALTER TABLE capture ALTER COLUMN user_id DROP DEFAULT;
ALTER TABLE capture
    ADD KEY idx_capture_user_id (user_id, id),
    ADD CONSTRAINT fk_capture_user FOREIGN KEY (user_id) REFERENCES app_user (id);
