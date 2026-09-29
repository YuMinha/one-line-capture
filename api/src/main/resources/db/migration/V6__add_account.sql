-- 로그인·기기 연결 (stack.md §2.1, §5)

-- 사용자 1명 = 토큰 여러 개(기기마다 하나). app_user에 토큰이 하나뿐이면 기기 하나만 로그아웃할 수 없다
CREATE TABLE device_token (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    token_hash  VARCHAR(64) NOT NULL,
    -- 주인의 API_TOKEN에서 온 토큰. 기동할 때 OwnerTokenSync가 이 행만 갈아끼운다
    from_env    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_token_hash (token_hash),
    KEY idx_device_token_user (user_id),
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 이미 나간 토큰(주인·게스트)이 배포 직후에도 그대로 통해야 한다
INSERT INTO device_token (user_id, token_hash, from_env)
    SELECT id, token_hash, id = 1 FROM app_user WHERE token_hash IS NOT NULL;

ALTER TABLE app_user
    DROP INDEX uk_app_user_token_hash,
    DROP COLUMN token_hash,
    -- 셋 다 NULL이면 게스트다. 가입은 새 사용자를 만들지 않고 여기를 채운다 (기록 유지)
    ADD COLUMN login_id      VARCHAR(20)  NULL,
    ADD COLUMN email         VARCHAR(254) NULL,
    ADD COLUMN password_hash VARCHAR(200) NULL,
    ADD UNIQUE KEY uk_app_user_login_id (login_id),
    ADD UNIQUE KEY uk_app_user_email (email);

-- 코드 원문도 토큰처럼 해시로만 남긴다. DB가 새도 살아 있는 코드를 쓸 수 없게
CREATE TABLE link_code (
    code_hash   VARCHAR(64) NOT NULL,
    user_id     BIGINT      NOT NULL,
    expires_at  TIMESTAMP   NOT NULL,
    PRIMARY KEY (code_hash),
    KEY idx_link_code_user (user_id),
    CONSTRAINT fk_link_code_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
