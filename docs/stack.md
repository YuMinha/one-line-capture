# stack.md — 기술 스택 · 데이터 모델 · API

> 최종 수정: 2026-08-25 / 대상: 한 줄 캡처 앱 v1

## 0. 70 / 30 배분

이번 프로젝트의 학습 예산은 **"내가 아는 것 70% + 새로 배울 것 30%"** 로 잡았다.
비율을 지키는 이유는 하나다 — 최우선 목표가 "일단 하나 제대로 완성해보기"이기 때문이다.
새 기술이 늘어날수록 완성 확률은 곱셈으로 떨어진다.

### 아는 것 (70%) — 여기서 시간을 아낀다

| 영역 | 선택 | 근거 |
|---|---|---|
| 언어·프레임워크 | Java + Spring Boot 3 | 이미 손에 익음. 4주 내내 문법 검색으로 시간 안 씀 |
| 데이터 접근 | Spring Data JPA | 익숙한 조합. CRUD 코드가 거의 공짜 |
| DB | **MySQL 8** | 유일하게 써본 DB. SQL을 직접 쓸 수 있는 게 이 프로젝트의 강점 |
| 프론트엔드 | 바닐라 JS + Vite | HTML/CSS/JS는 이미 할 줄 앎. 화면 3개엔 프레임워크가 과함 |

### 새로 배울 것 (30%) — 여기에 학습 시간을 몰아준다

| 영역 | 선택 | 왜 지금 배울 가치가 있나 |
|---|---|---|
| **컨테이너** | Docker + docker-compose | 아래 §0.1 |
| **배포** | 저가 VPS + Caddy(HTTPS) | 아래 §0.1 |
| 마이그레이션 | Flyway | 아래 §0.2 (작은 추가) |
| 빌드 도구 | Vite | 아래 §0.2 (작은 추가) |

### 0.1 왜 Docker와 배포가 "지금" 배울 가치가 있나

**첫째, 이 프로젝트의 목표가 그것을 요구한다.**
"장기적으로 출시까지"라고 했다. 출시는 코드를 잘 짜는 문제가 아니라 **남의 컴퓨터에서
돌게 만드는 문제**다. `docker-compose.yml` 한 파일로 앱과 DB가 같이 뜨는 경험은
"내 노트북에서는 되는데요"에서 벗어나는 첫 관문이고, 이 관문을 통과하지 않으면
아무리 코드를 잘 짜도 출시는 영원히 다음 프로젝트가 된다.

**둘째, 지금이 배우기 가장 싼 시점이다.**
Docker의 학습 비용은 **서비스 개수에 비례**한다. 지금은 앱 1개 + DB 1개, 즉 최소 구성이다.
나중에 캐시·큐·워커가 붙은 상태에서 처음 배우면 볼륨이 왜 안 붙는지, 네트워크가 왜 안 닿는지
원인을 좁힐 수가 없다. **틀려도 안전한 크기일 때 틀려보는 게 학습이다.**

**셋째, 백엔드를 깊게 배우겠다는 목표와 정확히 맞물린다.**
백엔드의 절반은 "요청을 받아 응답하는 코드"지만, 나머지 절반은 **환경**이다.
포트, 환경변수, 프로세스 수명, 로그, 볼륨, 재시작 정책, 리버스 프록시, 인증서.
이건 강의로 안 익혀지고 한 번 직접 올려봐야 몸에 남는다. 그리고 이 절반이
실무에서 신입과 경력을 가르는 지점이다.

**넷째, 하나 만들어 놓으면 평생 재사용된다.**
잘 만든 `docker-compose.yml` + `.env.example` + README 3종 세트는 다음 프로젝트에
그대로 복사된다. 이번에 제대로 만들어두면 다음부터 배포는 30분짜리 일이 된다.

> **반대 근거도 알아두기:** Docker 없이도 VPS에 JAR 하나 올리고 systemd로 띄우면 된다.
> 실제로 그게 더 빠르다. Docker를 택하는 건 "지금 이 프로젝트에 필요해서"가 아니라
> **"이번에 이걸 배우기로 정했기 때문"** 이다. 이 구분은 정직하게 해두는 게 좋다.

### 0.2 왜 Flyway와 Vite는 추가해도 되나

30%를 넘기지 않으려면 새 기술을 더 넣지 말아야 하는데, 이 둘은 예외로 둔다.
**"배워야 할 개념"이 아니라 "설정 한 번 하고 잊는 도구"** 이기 때문이다.

- **Flyway** — `V1__create_capture.sql` 같은 파일을 순서대로 실행해줄 뿐이다. 개념은 30분.
  대신 이걸 안 쓰고 `ddl-auto: update`로 가면 **배포한 서버의 스키마를 바꿀 방법이 없어진다.**
  로컬에서만 돌 거면 없어도 되지만, 배포가 목표라면 사실상 필수다.
- **Vite** — `npm create vite` 하고 `dev`/`build` 두 명령만 쓴다. 학습량이 사실상 0인데
  개발 중 자동 새로고침과 환경변수(`VITE_API_BASE_URL`) 주입을 공짜로 준다.

### 0.3 명시적으로 배제한 것

| 안 쓰는 것 | 이유 |
|---|---|
| React / Vue | 화면 3개엔 과하다. 30%를 Docker에 몰아주기로 했다 |
| PostgreSQL | MySQL을 이미 안다. DB 방언까지 새로 배우면 30%가 45%가 된다 |
| Spring Security | v1 인증은 토큰 1개다. 필터 하나로 끝나는 걸 프레임워크로 감쌀 이유가 없다 |
| QueryDSL / MyBatis | JPA + 필요할 때 native query로 v1은 충분하다 |
| Redis / 메시지 큐 | 캐시할 게 없고 비동기로 미룰 일이 없다 |
| Kubernetes | 서버 1대에 컨테이너 3개다. 진심으로 필요 없다 |

---

## 1. 전체 구조

```
┌─────────────────────────────────────────────┐
│  브라우저 (PWA)                              │
│  Vanilla JS + Vite → 정적 파일               │
└──────────────────┬──────────────────────────┘
                   │ HTTPS, X-API-Token 헤더
┌──────────────────▼──────────────────────────┐
│  Caddy (리버스 프록시 · 자동 HTTPS)           │
│   /        → web 컨테이너 (nginx, 정적파일)   │
│   /api/*   → api 컨테이너 (Spring Boot)      │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│  api : Spring Boot 3 (Java 21)              │
│    Controller → Service → Repository        │
│                    └→ CaptureParser         │
└──────────────────┬──────────────────────────┘
                   │ JDBC
┌──────────────────▼──────────────────────────┐
│  db : MySQL 8  (named volume에 데이터 영속)   │
└─────────────────────────────────────────────┘
```

컨테이너 4개: `caddy`, `web`, `api`, `db`. 전부 하나의 `docker-compose.yml`에 있다.

### 패키지 구조 (api)

```
com.example.capture
├─ capture/
│   ├─ CaptureController.java
│   ├─ CaptureService.java
│   ├─ CaptureRepository.java
│   └─ domain/  Capture, Expense, Todo, Link, CaptureType
├─ parser/
│   ├─ CaptureParser.java        // 룰들을 순서대로 시도
│   ├─ ParseRule.java            // 인터페이스
│   ├─ ParsedCapture.java        // 파싱 결과 DTO
│   └─ rule/  LinkRule, ExpenseRule, TodoRule
├─ summary/
│   ├─ SummaryController.java
│   └─ SummaryRepository.java    // 여기만 native SQL
└─ common/
    ├─ ApiTokenFilter.java
    ├─ GlobalExceptionHandler.java
    └─ config/  ClockConfig, WebConfig
```

기능별로 묶었다(`controller/`, `service/` 처럼 계층별로 나누지 않았다).
파서를 고칠 때 `parser/` 폴더만 열면 되는 구조가 이 프로젝트엔 낫다.

---

## 2. 데이터 모델

### 선택한 구조: 공통 테이블 + 타입별 상세 테이블 (1:1)

```
        capture (공통: 원문, 타입, 시각)
       ┌────┴─────┬──────────┐
       │          │          │
   expense       todo       link      ← capture_id가 PK이자 FK
```

**왜 이 구조인가**

- `amount`가 진짜 `DECIMAL` 컬럼이라 **월별 합계가 `SUM(amount)` 한 줄**로 끝난다.
  JSON에 넣었다면 파싱 후 캐스팅해야 하고, 문자열이 섞여 들어가도 DB가 못 막는다.
- `due_at`이 진짜 날짜 컬럼이라 "마감 지난 할일" 정렬에 인덱스가 먹는다.
- 공통 테이블이 있어서 "최근 캡처 전체 보기"가 `capture` 한 번 조회로 끝난다.

**대가로 감수하는 것**

- 초기 코드량이 가장 많다 (테이블 4개, 엔티티 4개, 마이그레이션 4개)
- 타입을 추가하면 테이블도 늘어난다 — 다만 §안 만들 것에서 타입은 3개로 고정했으므로 문제되지 않는다
- 목록 조회 시 상세를 같이 가져오려면 조인이 필요하다 → **N+1 주의** (§2.5)

### 2.1 DDL

```sql
-- V1__create_capture.sql
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
```

```sql
-- V2__create_expense.sql
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
```

```sql
-- V3__create_todo.sql
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
```

```sql
-- V4__create_link.sql
CREATE TABLE link (
    capture_id  BIGINT        NOT NULL,
    url         VARCHAR(1000) NOT NULL,
    note        VARCHAR(300)  NULL,                 -- URL 앞뒤에 붙은 설명 텍스트
    read_at     TIMESTAMP     NULL,                 -- NULL이면 안 읽음
    PRIMARY KEY (capture_id),
    CONSTRAINT fk_link_capture FOREIGN KEY (capture_id)
        REFERENCES capture(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2 설계 결정 메모 (나중에 "왜 이렇게 했지?" 할 때 볼 것)

- **`raw_text`를 지우지 않는다.** 파서를 고친 뒤 과거 데이터를 다시 파싱해볼 수 있다.
  이게 있으면 파서 개선이 안전해지고, 없으면 한 번 잘못 파싱된 건 영영 못 고친다.
- **`amount`는 `DECIMAL`.** `DOUBLE`은 `0.1 + 0.2 != 0.3`이라 돈에 쓰면 안 된다.
  원화 정수만 쓸 거라 `BIGINT`도 가능하지만, `DECIMAL`이 다른 프로젝트에도 그대로 옮겨지는 습관이다.
- **모든 시각은 `TIMESTAMP` + UTC 저장.** MySQL `TIMESTAMP`는 저장 시 UTC로 변환된다.
  앱 JVM 타임존도 UTC로 고정하고(`TZ=UTC`), **화면에 그릴 때만 KST로 바꾼다.**
  이걸 안 하면 배포한 서버와 로컬의 "오늘"이 달라져서 월별 요약이 틀린다.
- **`utf8mb4` 필수.** `utf8`(3바이트)로 두면 이모지가 들어올 때 저장이 깨진다.
- **`ON DELETE CASCADE`** — capture를 지우면 상세도 같이 사라진다. 고아 행 걱정을 DB에 맡긴다.
- **`source` 컬럼** — 자동 분류가 얼마나 자주 틀리는지 나중에 세어볼 수 있다.
  `SELECT source, COUNT(*) FROM capture GROUP BY source` 한 줄로 파서 정확도가 나온다.

### 2.3 JPA 매핑에서 주의할 점

상세 테이블의 PK가 곧 FK인 구조는 JPA에서 `@MapsId`로 매핑한다.

```java
@Entity
public class Expense {
    @Id
    private Long captureId;              // 별도 생성 안 함

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId                              // capture의 id를 이 엔티티의 id로 그대로 사용
    @JoinColumn(name = "capture_id")
    private Capture capture;

    private BigDecimal amount;
    private String merchant;
    private LocalDate spentAt;
}
```

`@MapsId` 없이 `@OneToOne`만 쓰면 JPA가 별도 PK를 만들려 해서 스키마와 어긋난다.
**이번 프로젝트에서 JPA로 배울 가장 중요한 한 가지가 이거다.**

### 2.4 대안 검토 (기록용)

| 대안 | 이 프로젝트에서 안 쓴 이유 |
|---|---|
| 단일 테이블 + JSON 컬럼 | 완성은 가장 빠르지만, 월별 집계에서 `JSON_EXTRACT` + 캐스팅이 필요하고 DB가 값을 검증 못 한다. "제대로 완성"이 목표라 탈락 |
| 타입별 독립 3테이블 | "최근 캡처 전체 보기"가 3-way UNION이 되고 `raw_text`/`created_at`이 3번 중복된다 |
| 단일 테이블 + 널 허용 컬럼 전부 | 컬럼 절반이 항상 NULL. 제약조건을 하나도 걸 수 없다 |

### 2.5 성능 함정 (미리 알아두기)

목록 조회에서 capture 20건을 가져온 뒤 각각의 상세를 lazy 로딩하면 **쿼리가 21번** 나간다(N+1).
v1은 데이터가 적어서 체감이 없지만, 습관을 들이는 차원에서 조회 시 `JOIN FETCH`를 쓴다.
단, 타입이 섞인 목록은 한 번에 fetch join이 안 되므로 **v1에서는 타입 필터가 있을 때만 fetch join,
전체 조회는 상세 없이 요약 정보만** 내려주는 것으로 단순화한다.

---

## 3. API 초안

- Base URL: `/api/v1`
- 인증: 모든 엔드포인트에 `X-API-Token: <토큰>` 헤더 필수 (`/api/v1/health` 제외)
- 시각 포맷: ISO-8601 UTC (`2026-08-25T04:30:00Z`)
- 금액: 숫자 (문자열 아님)

### 3.1 엔드포인트 목록

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/v1/health` | 헬스체크. 인증 없음. compose healthcheck가 씀 |
| `POST` | `/api/v1/captures` | **한 줄 저장 (핵심 API)** |
| `POST` | `/api/v1/captures/preview` | 저장 없이 파싱 결과만 확인 (파서 개발·디버깅용) |
| `GET` | `/api/v1/captures` | 목록 조회 (타입 필터, 커서 페이징) |
| `GET` | `/api/v1/captures/{id}` | 단건 조회 |
| `PATCH` | `/api/v1/captures/{id}` | **분류 수정** (타입 변경 + 상세 필드 수정) |
| `DELETE` | `/api/v1/captures/{id}` | 삭제 (상세는 CASCADE) |
| `PATCH` | `/api/v1/todos/{captureId}` | 할일 완료 토글 |
| `PATCH` | `/api/v1/links/{captureId}` | 링크 읽음 토글 |
| `GET` | `/api/v1/summary/expenses` | 월별 지출 요약 |

### 3.2 POST /api/v1/captures

```http
POST /api/v1/captures
X-API-Token: {token}
Content-Type: application/json

{ "text": "점심 9000원" }
```

```json
201 Created
{
  "id": 142,
  "type": "EXPENSE",
  "rawText": "점심 9000원",
  "source": "AUTO",
  "createdAt": "2026-08-25T04:30:00Z",
  "expense": {
    "amount": 9000,
    "merchant": "점심",
    "spentAt": "2026-08-25"
  }
}
```

타입에 해당하는 상세 객체만 포함된다 (`expense` / `todo` / `link` 중 하나).
바닐라 JS에서 `if (item.expense) {...}` 로 분기하면 되므로 다루기 쉽다.

에러:

```json
400 Bad Request
{ "error": { "code": "TEXT_REQUIRED", "message": "text는 비어 있을 수 없습니다" } }
```

### 3.3 GET /api/v1/captures

```
GET /api/v1/captures?type=EXPENSE&cursor=142&size=20
```

| 파라미터 | 기본값 | 설명 |
|---|---|---|
| `type` | 없음(전체) | `EXPENSE` / `TODO` / `LINK` |
| `cursor` | 없음 | 이 id보다 작은 것부터 (최신순) |
| `size` | 20 | 최대 50 |
| `done` | 없음 | `type=TODO`일 때만. `false`면 미완료만 |

```json
200 OK
{
  "items": [ { ...capture... }, ... ],
  "nextCursor": 122,
  "hasNext": true
}
```

> **왜 offset 페이징이 아니라 커서인가:** 목록이 최신순이고 위에서 계속 추가되는 구조라
> offset은 스크롤 중 항목이 밀려 중복·누락이 생긴다. 커서는 그 문제가 없고, 인덱스도 잘 탄다.
> v1 데이터 규모에선 offset도 무방하지만 배우기 좋은 지점이라 커서로 간다.

### 3.4 PATCH /api/v1/captures/{id} — 분류 수정

앱에서 가장 신경 써야 할 API다. **타입 변경 = 기존 상세 행 삭제 + 새 타입 상세 행 생성**이므로
반드시 하나의 트랜잭션 안에서 처리한다.

```http
PATCH /api/v1/captures/142
{
  "type": "TODO",
  "todo": { "title": "점심 약속", "dueAt": "2026-08-26T04:00:00Z" }
}
```

동작 규칙:

1. `type`이 그대로면 → 상세 필드만 업데이트
2. `type`이 바뀌면 → 기존 상세 행 삭제, 새 상세 행 삽입, `capture.type` 변경
3. 어느 경우든 `source`를 `MANUAL`로 바꾼다 (사용자가 손댔음을 기록)
4. `rawText`는 절대 바뀌지 않는다

### 3.5 GET /api/v1/summary/expenses

```
GET /api/v1/summary/expenses?month=2026-08
```

```json
200 OK
{
  "month": "2026-08",
  "totalAmount": 412500,
  "count": 63,
  "dailyTotals": [
    { "date": "2026-08-01", "amount": 12000, "count": 2 },
    { "date": "2026-08-02", "amount": 9000,  "count": 1 }
  ]
}
```

이 부분만 JPA 대신 native SQL을 쓴다. 집계는 SQL이 훨씬 명확하고, 이미 잘하는 영역이다.

```sql
SELECT DATE(e.spent_at)      AS d,
       SUM(e.amount)         AS amount,
       COUNT(*)              AS cnt
FROM expense e
WHERE e.spent_at >= :monthStart
  AND e.spent_at <  :nextMonthStart
GROUP BY DATE(e.spent_at)
ORDER BY d;
```

> `DATE_FORMAT(spent_at, '%Y-%m') = '2026-08'` 로 쓰면 **컬럼에 함수가 걸려 인덱스를 못 탄다.**
> 위처럼 범위 조건(`>=`, `<`)으로 쓰면 `idx_expense_spent_at`을 탄다. 실무에서 자주 나오는 함정이다.

---

## 4. 파서 설계

### 구조

```java
public interface ParseRule {
    Optional<ParsedCapture> tryParse(String raw, LocalDateTime now);
}
```

`CaptureParser`가 룰을 **순서대로** 시도하고, 첫 성공을 채택한다. 전부 실패하면 fallback.

```
1. LinkRule    — 텍스트에 http:// 또는 https:// 포함
2. ExpenseRule — 금액 패턴 발견
3. TodoRule    — 날짜/시간 표현 발견
4. (fallback)  — TODO, due_at = NULL
```

**순서가 중요한 이유:** URL 안에는 숫자가 흔해서(`.../post/9000`) ExpenseRule을 먼저 두면
링크가 지출로 분류된다. 링크 판정이 가장 확실하므로 맨 앞에 둔다.

### `now`를 인자로 받는 이유

`LocalDateTime.now()`를 룰 안에서 직접 부르면 **"내일 3시"를 테스트할 수 없다.**
`now`를 밖에서 주입하면 "2026-08-25 10:00에 '내일 3시'를 넣으면 2026-08-26 15:00이 나온다"를
테스트로 고정할 수 있다. 운영 코드에서는 Spring `Clock` 빈에서 받는다.

```java
@Bean
Clock clock() { return Clock.systemUTC(); }   // 테스트에서는 Clock.fixed(...)
```

### 룰별 인식 대상 (v1)

**LinkRule** — `https?://\S+` 를 추출하고, 나머지 텍스트를 `note`로.

**ExpenseRule** — 다음 중 하나가 매칭되면 지출로 판정하고 `merchant`는 금액 표현을 제거한 나머지.

| 패턴 | 예 | 결과 |
|---|---|---|
| `숫자 + 원` | `9000원`, `9,000원` | 9000 |
| `숫자 + 만원` | `1만원`, `1.5만원` | 10000, 15000 |
| `숫자 + 천원` | `5천원` | 5000 |
| `만/천` 단독 | `만원` | 10000 |

**TodoRule** — 아래 표현을 찾아 `due_at` 계산. `title`은 날짜 표현을 제거한 나머지.

| 종류 | 예 |
|---|---|
| 상대 날짜 | `오늘`, `내일`, `모레`, `다음주` |
| 절대 날짜 | `9/2`, `9월 2일` |
| 요일 | `금요일`, `이번주 금요일` |
| 시각 | `3시`, `오후 3시`, `15시`, `3시30분` |

날짜만 있고 시각이 없으면 그날 09:00, 시각만 있고 날짜가 없으면 오늘(이미 지났으면 내일)로 둔다.
**이 기본값 규칙은 문서에 적어두고 테스트로 고정한다** — 안 그러면 나중에 왜 이렇게 동작하는지 모른다.

### 테스트 전략

파서는 **입력 문자열 → 기대 결과**가 명확해서 테스트를 붙이기 가장 좋은 코드다.
`@ParameterizedTest` + `@CsvSource`로 케이스를 표처럼 쌓는다. v1 목표는 **30케이스 이상.**

반드시 넣을 케이스: 빈 문자열, 공백만, URL만, URL+한글, 금액 0원, 금액에 콤마,
`1.5만원`, `내일`만, `3시`만, 날짜+시각 조합, 아무것도 안 걸리는 문장, 500자 초과.

---

## 5. 인증 (v1)

`ApiTokenFilter` 하나. `/api/**` 요청에서 `X-API-Token` 헤더를 환경변수 `API_TOKEN`과 비교하고,
다르면 401. `/api/v1/health`는 예외.

```java
if (!MessageDigest.isEqual(
        header.getBytes(UTF_8), expected.getBytes(UTF_8))) {   // 타이밍 공격 방지
    response.setStatus(401);
    return;
}
```

토큰은 프론트에서 `localStorage`에 저장한다. **PWA를 처음 열 때 토큰 입력 화면 한 번**,
이후에는 저장된 값을 헤더에 붙인다.

> 이 방식의 한계는 정직하게 알아두자: XSS가 있으면 토큰이 털린다. 사용자가 나 하나이고
> 외부 스크립트를 안 붙이므로 v1에선 감수한다. v2에서 다중 사용자로 가면 이 자리는
> 세션 쿠키(HttpOnly) 또는 JWT + refresh로 교체된다.

---

## 6. 환경변수

`.env.example` (레포에 커밋). 실제 `.env`는 `.gitignore`에 넣는다.

```dotenv
# --- DB ---
MYSQL_DATABASE=capture
MYSQL_USER=capture
MYSQL_PASSWORD=change-me
MYSQL_ROOT_PASSWORD=change-me-too

# --- API ---
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/capture?characterEncoding=utf8mb4&serverTimezone=UTC
API_TOKEN=change-me-to-a-long-random-string
TZ=UTC

# --- Web ---
VITE_API_BASE_URL=http://localhost:8080/api/v1

# --- 배포 시에만 ---
DOMAIN=capture.example.com
```

**API 주소를 환경변수로 빼는 게 왜 중요한가:** 프론트 코드에 `http://localhost:8080`이 박혀 있으면
배포하는 순간 전부 찾아 고쳐야 하고, 반드시 하나를 빠뜨린다. `VITE_API_BASE_URL` 하나로
로컬/배포가 갈리게 해두면 배포가 설정 파일 한 줄 문제가 된다.

---

## 7. 아직 안 정한 것

문서에 적어두는 이유: 나중에 "이건 왜 이래?" 할 때 **잊은 게 아니라 미룬 것**임을 알기 위해서.

- **fallback을 TODO로 둘지, MEMO 타입을 추가할지.** v1은 TODO로 간다(spec.md §6).
  실제로 2주쯤 써보고 "할일이 아닌 게 할일에 너무 많이 쌓인다" 싶으면 그때 재검토.
- **`spent_at`을 텍스트에서 파싱할지.** "어제 점심 9000원" → 어제 날짜. v1은 항상 입력일.
  ExpenseRule에 TodoRule의 날짜 파서를 재사용하면 되므로 나중에 추가가 쉽다.
- **목록 전체 조회 시 상세를 내려줄지.** §2.5대로 v1은 안 내려준다. 화면이 요약만 보여주면 충분한지
  3주차에 실제로 보고 판단.
