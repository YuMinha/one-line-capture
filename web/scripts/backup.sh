#!/bin/sh
# MySQL 백업 하나. cron에 걸어 쓴다.
#
#   ./scripts/backup.sh              backups/ 에 저장
#   BACKUP_DIR=/mnt/x ./scripts/backup.sh
#
# 복원은 README의 "백업과 복원" 참고.

set -eu

cd "$(dirname "$0")/.."

BACKUP_DIR="${BACKUP_DIR:-backups}"
KEEP="${KEEP:-14}"

mkdir -p "$BACKUP_DIR"
OUT="$BACKUP_DIR/capture-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"

# 비밀번호를 호스트 명령줄에 올리지 않는다. db 컨테이너는 이미 MYSQL_* 를 갖고 있으므로
# 컨테이너 안에서 자기 환경변수를 읽게 한다. ps로도 안 보인다
#
#   --single-transaction  InnoDB를 잠그지 않고 일관된 시점을 뜬다. 백업 중에도 앱이 돈다
#   --no-tablespaces      PROCESS 권한이 없는 일반 계정으로 뜨려면 필요하다
docker compose exec -T db sh -c '
  MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump \
    --single-transaction \
    --no-tablespaces \
    --default-character-set=utf8mb4 \
    -u"$MYSQL_USER" "$MYSQL_DATABASE"
' | gzip > "$OUT"

# 빈 파일을 백업이라고 남겨두면 정작 필요할 때 알게 된다. 지금 확인한다
if ! gzip -t "$OUT" 2>/dev/null || [ ! -s "$OUT" ]; then
  echo "백업 실패: $OUT 이 비었거나 깨졌다" >&2
  rm -f "$OUT"
  exit 1
fi

if ! gzip -dc "$OUT" | grep -q 'CREATE TABLE'; then
  echo "백업 실패: $OUT 에 테이블이 없다" >&2
  rm -f "$OUT"
  exit 1
fi

echo "백업 완료: $OUT ($(wc -c < "$OUT") bytes)"

# 오래된 것부터 지운다. 디스크가 차면 백업도 앱도 같이 멈춘다
ls -1t "$BACKUP_DIR"/capture-*.sql.gz 2>/dev/null | tail -n "+$((KEEP + 1))" | while read -r old; do
  rm -f "$old"
  echo "오래된 백업 삭제: $old"
done
