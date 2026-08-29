-- 테스트는 개발용 DB를 건드리면 안 된다. @Transactional은 테스트가 만든 행만
-- 되돌리므로, 개발 중 남긴 데이터가 있으면 "빈 목록" 같은 단언이 깨진다
CREATE DATABASE IF NOT EXISTS capture_test
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON capture_test.* TO 'capture'@'%';
