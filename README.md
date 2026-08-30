# 한 줄 캡처

입력창에 한 줄만 던지면 **지출 / 할일 / 링크**로 자동 분류해 저장하는 개인 기록 앱.

```
점심 9000원                      → 지출   9,000원 · 점심
내일 3시 과제 제출               → 할일   마감 8/31 15:00
https://spring.io/... 스프링 정리 → 링크   URL + 메모
우산 챙기기                      → 할일   마감 없음   (아무 규칙에도 안 걸리면 할일)
```

기존 앱은 "무엇을 기록할지"를 먼저 고르게 한다. 그 선택 자체가 마찰이라 결국 아무 데도 안 남는다.
이 앱은 순서를 뒤집는다 — **일단 던지고, 분류는 앱이 한다.** 틀리면 목록에서 탭 두 번으로 고친다.

---

## 빠른 시작

필요한 건 **Docker**뿐이다. Java도 Node도 설치할 필요 없다.

```bash
git clone https://github.com/YuMinha/one-line-capture.git
cd one-line-capture

cp .env.example .env
# .env를 열어 MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD, API_TOKEN을 바꾼다

docker compose up -d --build
```

DB와 API가 준비될 때까지 기다렸다가 돌아온다. 처음 한 번은 이미지를 받고 빌드하느라 몇 분 걸린다.

브라우저에서 **http://localhost:8081** 을 열고, `.env`에 넣은 `API_TOKEN` 값을 입력하면 된다.

정상인지 확인:

```bash
curl localhost:8081/api/v1/health
# {"status":"UP"}
```

### 포트가 이미 쓰이고 있다면

`.env`의 포트 값만 바꾸면 된다. 컨테이너 **안쪽** 포트는 그대로다.

| 변수 | 기본값 | 무엇 |
|---|---|---|
| `WEB_PORT` | 8081 | 브라우저로 접속할 주소 |
| `API_PORT` | 8080 | API를 직접 찌를 때만 필요 |
| `MYSQL_PORT` | 3306 | DB에 직접 붙을 때만 필요 |

### 끄기

```bash
docker compose down      # 데이터는 남는다
docker compose down -v   # 볼륨까지 삭제 (기록이 전부 사라진다)
```

---

## 환경변수 (`.env`)

`.env.example`을 복사해서 쓴다. **`.env`는 절대 커밋하지 않는다** (`.gitignore`에 있다).

| 변수 | 설명 |
|---|---|
| `MYSQL_DATABASE` / `MYSQL_USER` | DB 이름과 계정 |
| `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` | **반드시 바꾼다** |
| `API_TOKEN` | 이 값을 아는 사람만 API를 쓸 수 있다. **길고 무작위로** |
| `SPRING_DATASOURCE_URL` | 앱을 로컬(IDE·`java -jar`)에서 띄울 때만 쓴다. compose로 띄운 api는 `db:3306`을 직접 받는다 |
| `TEST_DATASOURCE_URL` | `./gradlew test` 전용 스키마. 개발용 DB를 건드리지 않는다 |
| `VITE_API_BASE_URL` | 기본값 `/api/v1`. 상대경로라 CORS 설정이 필요 없다 |
| `TZ` | `UTC` 고정. 저장은 UTC, 화면에만 KST |

---

## API

모든 경로에 `X-API-Token` 헤더가 필요하다. **`/health`만 예외.**
시각은 ISO-8601 UTC, 금액은 숫자다.

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/v1/health` | 헬스체크 (인증 없음) |
| `POST` | `/api/v1/captures` | **한 줄 저장** |
| `POST` | `/api/v1/captures/preview` | 저장 없이 파싱 결과만 |
| `GET` | `/api/v1/captures` | 목록 (`type`, `cursor`, `size`, `done`) |
| `GET` | `/api/v1/captures/{id}` | 단건 조회 |
| `PATCH` | `/api/v1/captures/{id}` | 분류 수정 (타입 변경 + 필드 수정) |
| `DELETE` | `/api/v1/captures/{id}` | 삭제 (상세는 CASCADE) |
| `PATCH` | `/api/v1/todos/{captureId}` | 할일 완료 토글 |
| `PATCH` | `/api/v1/links/{captureId}` | 링크 읽음 토글 |
| `GET` | `/api/v1/summary/expenses?month=2026-08` | 월별 지출 요약 |

```bash
TOKEN=$(grep '^API_TOKEN=' .env | cut -d= -f2)

curl -X POST localhost:8081/api/v1/captures \
  -H "X-API-Token: $TOKEN" -H 'Content-Type: application/json' \
  -d '{"text":"점심 9000원"}'
```

```json
{
  "id": 142, "type": "EXPENSE", "rawText": "점심 9000원",
  "source": "AUTO", "createdAt": "2026-08-30T04:30:00Z",
  "expense": { "amount": 9000, "merchant": "점심", "spentAt": "2026-08-30" }
}
```

타입에 해당하는 상세 객체 하나만 들어 있다. 프론트는 `if (item.expense)`로 분기한다.

에러는 어떤 경우든 모양이 같다:

```json
{ "error": { "code": "TEXT_REQUIRED", "message": "text는 비어 있을 수 없습니다" } }
```

---

## 스택과 그 이유

| 영역 | 선택 | 왜 |
|---|---|---|
| 백엔드 | Java 21 · Spring Boot 3 | 이미 손에 익어서. 문법 검색에 시간을 안 쓴다 |
| 데이터 | MySQL 8 · Spring Data JPA · Flyway | 집계만 native SQL. 스키마 변경은 전부 마이그레이션 파일로 |
| 프론트 | **바닐라 JS + Vite** | 화면 3개에 프레임워크는 과하다. 런타임 의존성 0개 |
| 인증 | `X-API-Token` + 서블릿 필터 1개 | 토큰 하나 비교에 Spring Security는 과하다 |
| 배포 | Docker Compose (`web` / `api` / `db`) | "내 노트북에서는 되는데요"에서 벗어나는 게 이 프로젝트의 학습 목표 |

**"익숙한 것 70% + 새로 배울 것 30%"** 로 정했다. 새 기술이 늘수록 완성 확률은 곱셈으로 떨어진다.
자세한 근거는 [`docs/stack.md`](docs/stack.md) §0.

### 파서

규칙 기반이다. 룰을 순서대로 시도하고 첫 성공을 채택한다.

```
1. LinkRule    http:// 또는 https:// 포함
2. ExpenseRule 금액 패턴 (9000원 / 5,500원 / 1만원 / 5천원 / 1.5만원)
3. TodoRule    날짜·시각 (오늘·내일·모레·다음주 / 9월 2일 / 9/2 / 금요일 / 오후 3시)
4. fallback    마감 없는 할일
```

순서가 중요하다. URL 안에는 숫자가 흔해서(`.../post/9000`) ExpenseRule을 앞에 두면 링크가 지출이 된다.

**규칙 기반은 틀린다.** `점심 오백원`은 한글 숫자를 몰라 할일로 떨어진다.
정확도를 올리는 대신 **고치기 쉽게** 만들었다 — 목록에서 항목을 탭하면 타입과 필드를 바꿀 수 있다.

---

## 개발

컨테이너 없이 직접 돌릴 때.

```bash
docker compose up -d db          # DB만 띄운다

cd api && ./gradlew test         # 백엔드 테스트 (전용 스키마 capture_test 사용)
cd web && npm install && npm run dev   # 프론트 개발 서버 (http://localhost:5173)
cd web && npm test               # 프론트 테스트 (node --test, 프레임워크 없음)
```

`npm run dev`는 `/api`를 API 컨테이너로 프록시한다. 개발에서도 같은 출처라 CORS 설정이 없다.

### 구조

```
one-line-capture/
├─ docker-compose.yml          web / api / db
├─ .env.example                실제 .env는 gitignore
├─ db/init/                    테스트용 스키마 생성 (볼륨 첫 생성 시 1회)
├─ docs/                       spec.md · stack.md · tasks.md
├─ api/                        Spring Boot
│   └─ src/main/java/com/example/capture/
│       ├─ capture/            Controller · Service · Repository · domain
│       ├─ parser/             CaptureParser · ParseRule · rule/
│       ├─ summary/            집계 (여기만 native SQL)
│       └─ common/             ApiTokenFilter · GlobalExceptionHandler · config
└─ web/                        Vite + 바닐라 JS
    └─ src/                    api.js · format.js · pager.js · *-view.js
```

패키지는 계층별이 아니라 **기능별**로 나눴다. 파서를 고칠 때 `parser/` 폴더만 열면 된다.

### 설계에서 조심한 것들

- **시각** — 저장은 UTC, 화면은 KST, **파싱은 KST**. 셋을 섞으면 KST 새벽에 "내일 3시"가 15시간 어긋난다
- **돈** — `BigDecimal` + `DECIMAL(12,2)`. `double`은 `0.1 + 0.2 != 0.3`이라 돈에 못 쓴다
- **원문** — `raw_text`는 절대 지우거나 바꾸지 않는다. 파서를 고친 뒤 과거 데이터를 다시 파싱해볼 수 있다
- **토큰 비교** — `equals()`는 다른 글자가 나오면 즉시 멈춰 비교 시간으로 값을 추측당한다. `MessageDigest.isEqual`을 쓴다
- **N+1** — 목록은 상세 쪽에서 조회하며 `JOIN FETCH`. 5건 조회에 쿼리 1번인 것을 실측했다
- **집계 인덱스** — `DATE_FORMAT(spent_at, ...)`으로 쓰면 인덱스를 못 탄다. 범위 조건으로 쓴다 (`EXPLAIN` 확인)

---

## 백업과 복원

```bash
./scripts/backup.sh          # backups/capture-<시각>.sql.gz 로 저장
```

`--single-transaction` 이라 **백업 중에도 앱이 계속 돈다.** 비밀번호는 호스트 명령줄에
올리지 않고 db 컨테이너가 가진 환경변수를 그대로 쓴다(`ps`에 안 보인다).
빈 파일이나 깨진 gzip이면 즉시 실패한다 — 정작 필요할 때 알게 되면 늦다.
기본으로 최근 14개만 남긴다(`KEEP=30 ./scripts/backup.sh` 로 조절).

매일 새벽 4시에 자동 백업:

```bash
crontab -e
0 4 * * * cd /home/you/one-line-capture && ./scripts/backup.sh >> backups/backup.log 2>&1
```

복원 — **덮어쓰기 전에 지금 것을 먼저 백업하라.**

```bash
./scripts/backup.sh                    # 되돌릴 지점을 먼저 만든다

gzip -dc backups/capture-<시각>.sql.gz   | docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'

docker compose restart api
```

덤프에는 `flyway_schema_history`도 들어 있어서, 복원한 DB에 Flyway가 마이그레이션을
다시 돌리지 않는다.

> 복원해본 적 없는 백업은 백업이 아니다. 다른 스키마에 한 번 복원해서 행 수를 대조해보라.

## 배포 (VPS)

### 서버 사양

실측 사용량(유휴 상태, 기록 15건 기준):

| 컨테이너 | 사용 | 상한 |
|---|---|---|
| `db` (MySQL 8.4) | 456MB | 768MB |
| `api` (JVM) | 290MB | 640MB (힙 448MB) |
| `web` (nginx) | 13MB | 64MB |
| `caddy` | — | 128MB |

**메모리 2GB 이상을 권한다.** 1GB 서버(Oracle `VM.Standard.E2.1.Micro` 등)에서는
`docker-compose.1gb.yml`을 덮어써서 쓴다 — MySQL 버퍼 풀과 `performance-schema`를 줄이고
JVM 힙을 낮춰 셋이 724MB 안에 들어간다. 실측으로 확인한 값이다.

```bash
# .env 에 이 줄을 넣으면 docker compose up -d 만으로 적용된다 (리눅스 구분자는 콜론)
COMPOSE_FILE=docker-compose.yml:docker-compose.1gb.yml
```

1GB에서는 **swap을 반드시 잡는다.** 여유가 200MB뿐이라 순간적으로 넘칠 때 OOM으로 죽는다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab   # 재부팅 후에도 유지
free -h
```

디스크는 10GB면 충분하다(이미지 약 1.5GB + DB + 로그 최대 120MB).
CPU는 1코어로 충분하다 — 사용자가 한 명이다.

`mem_limit`이 없으면 JVM이 호스트 메모리의 25%를 힙으로 잡고 MySQL도 호스트를 보고
버퍼 풀을 키운다. 둘이 메모리를 다투다 OOM으로 죽는다. compose에 상한을 박아둔 이유다.

### 순서

로컬은 `web` 컨테이너(8081)로 바로 붙지만, 서버에서는 그 앞에 Caddy를 세워
**도메인 하나로 프론트와 API를 같이** 서비스하고 HTTPS를 자동으로 받는다.

```bash
# 서버에서
git clone https://github.com/YuMinha/one-line-capture.git
cd one-line-capture
cp .env.example .env
# .env에서 비밀값 3개와 DOMAIN을 채운다. DNS A 레코드가 이 서버를 가리켜야 한다

docker compose --profile deploy up -d --build
```

`--profile deploy` 를 줘야 Caddy가 함께 뜬다. 인증서는 Caddy가 Let's Encrypt에서
알아서 받고 갱신한다 — 설정 파일에 인증서 이야기가 없는 게 정상이다.

방화벽은 **22 / 80 / 443만** 열면 된다. `WEB_PORT`·`API_PORT`·`MYSQL_PORT`는 호스트에
바인딩되므로, 서버에서는 이 포트들을 방화벽에서 막아 두는 편이 안전하다.

> 인증서는 `caddy-data` 볼륨에 쌓인다. 이 볼륨을 날리면 재발급 한도(주당 5회)에 걸릴 수 있다.
> `docker compose down -v` 는 서버에서 절대 쓰지 말 것.

**도메인 없이 먼저 띄우려면** `--profile deploy` 없이 올리고 `http://서버IP:8081` 로 접속하면 된다.
HTTPS는 나중에 붙여도 앱은 그대로 동작한다.

## 재시작

세 컨테이너 모두 `restart: unless-stopped` 다. 서버가 재부팅되거나 앱이 죽으면 자동으로
다시 뜬다. 단 `docker compose stop` 으로 직접 멈춘 것은 다시 켜지 않는다 — 그게
`always` 가 아니라 `unless-stopped` 인 이유다.

---

## 아직 없는 것

[`docs/spec.md`](docs/spec.md) §5·§6에 "왜 안 만드는가"까지 적어뒀다.

**미룬 것:** 다중 사용자 · 소셜 로그인 · 푸시 알림 · 통계 차트 · LLM 분류
**안 만들 것:** 미분류함 · 저장 전 확인 화면 · 태그/폴더 · 카드 연동 · 첨부파일 · 네이티브 앱

---

## 라이선스

MIT — [`LICENSE`](LICENSE)
