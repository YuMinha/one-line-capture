# CLAUDE.md — 한 줄 캡처 앱

## 이 프로젝트

입력창에 한 줄만 던지면 **지출 / 할일 / 링크**로 자동 분류해 저장하는 개인 기록 앱.
개발자는 백엔드를 배우는 학생 1명. **최우선 목표는 "일단 하나 제대로 완성"** 이다.
빠른 완성보다 중요한 건 없고, 스코프를 늘리는 제안은 도움이 아니라 방해다.

## 작업 시작 전에 반드시 읽을 것

- `docs/spec.md` — 무엇을 만들고 **무엇을 안 만드는가**
- `docs/stack.md` — 스택 근거, DDL, API 스펙, 파서 설계
- `docs/tasks.md` — 주차별 작업 목록 (T1.1 ~ T4.9)

이 세 문서가 이 프로젝트의 유일한 진실이다.
문서와 다른 판단이 필요하면 **코드를 먼저 바꾸지 말고 문서 수정을 제안**하라.

---

## 작업 방식

1. **한 번에 태스크 하나만.** 시작할 때 `tasks.md`의 T번호를 명시한다.
2. **태스크가 끝나면 멈추고 보고한다.** 다음 태스크로 자동으로 넘어가지 않는다.
3. 한 태스크가 파일 10개 이상을 건드릴 것 같으면, 진행 전에 **쪼개자고 제안**한다.
4. 커밋은 태스크 단위. 메시지 형식: `T1.3 docker-compose에 MySQL 서비스 추가`
5. 태스크마다 **학습 노트 3줄**을 반드시 남긴다:
   - **무엇을** 만들었나
   - **왜** 그 방식인가 (버린 대안이 있으면 무엇을 왜 버렸나)
   - 이 코드에서 **나중에 문제가 될 수 있는 지점**

   개발자는 배우는 중이다. 이 3줄이 이 프로젝트에서 코드만큼 중요하다.
   설명은 짧고 구체적으로. "베스트 프랙티스라서"는 설명이 아니다.

6. 막히면 혼자 3번까지만 시도하고, 그 뒤엔 **무엇을 시도했고 무엇이 실패했는지** 보고한다.

---

## 고정된 결정 (임의로 바꾸지 말 것)

| 영역 | 결정 |
|---|---|
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.x, Gradle (Groovy DSL) |
| 데이터 접근 | Spring Data JPA (집계 쿼리만 native SQL) |
| DB | **MySQL 8** — utf8mb4, 시각은 전부 UTC 저장 |
| 마이그레이션 | Flyway. `ddl-auto`는 `validate` 고정 |
| 프론트엔드 | **바닐라 JS + Vite** — React/Vue/TypeScript 도입 금지 |
| 인증 | `X-API-Token` 헤더 + 서블릿 필터 1개. Spring Security 추가 금지 |
| 배포 | Docker Compose (`api` / `web` / `db` / `caddy`) |

이 결정들은 "익숙한 것 70% + 새로 배울 것 30%" 원칙으로 정해졌다. 근거는 `docs/stack.md` §0.
더 나은 기술이 있어도 바꾸지 않는다. **바꾸는 순간 학습 예산이 초과되고 완성이 멀어진다.**

---

## 절대 하지 말 것

- **새 의존성을 임의로 추가하지 않는다.** 필요하면 먼저 물어본다. (Lombok, Jackson 등 이미 있는 건 예외)
- **`ddl-auto: update` / `create` 금지.** 스키마 변경은 반드시 Flyway 마이그레이션 파일로.
- **`.env` 커밋 금지.** 비밀값(DB 비밀번호, API 토큰)을 코드나 `application.yml`에 하드코딩 금지.
  전부 환경변수로 읽고, `.env.example`에 더미값만 넣는다.
- **돈에 `double`/`float` 금지.** `BigDecimal` + `DECIMAL(12,2)`.
- **파서 룰 안에서 `LocalDateTime.now()` 호출 금지.** `Clock`을 주입받는다. 안 그러면 테스트가 불가능하다.
- **엔티티에 `@Data` / `@Setter` 금지.** Lombok은 `@Getter`, `@RequiredArgsConstructor`까지만.
- **엔티티를 컨트롤러 응답으로 그대로 반환 금지.** 응답은 `record` DTO로.
- **테스트 없이 파서 룰 추가 금지.** 룰 하나 = 테스트 케이스 최소 3개.
- **`spec.md` §6 "안 만들 것"에 있는 기능을 제안하지 않는다.**
  (미분류함, 저장 전 확인 화면, 태그/폴더, 카드 연동, 첨부파일, 네이티브 앱)
- **주석에 "무엇을 하는지" 쓰지 않는다.** 코드가 이미 말한다. 주석은 **"왜"** 만 적는다. 한국어로.

---

## 디렉터리 구조

```
one-line-capture/
├─ CLAUDE.md
├─ README.md
├─ docker-compose.yml
├─ .env.example              # 실제 .env는 gitignore
├─ docs/
│   ├─ spec.md  stack.md  tasks.md
├─ api/                      # Spring Boot
│   ├─ Dockerfile            # 멀티스테이지
│   └─ src/main/java/com/example/capture/
│       ├─ capture/          # Controller, Service, Repository, domain
│       ├─ parser/           # CaptureParser, ParseRule, rule/
│       ├─ summary/          # 집계 (여기만 native SQL)
│       └─ common/           # ApiTokenFilter, GlobalExceptionHandler, config
└─ web/                      # Vite + 바닐라 JS
    └─ Dockerfile            # 빌드 → nginx
```

패키지는 **계층별이 아니라 기능별**로 나눈다. 파서를 고칠 때 `parser/` 폴더만 열면 되게.

---

## 검증

태스크를 끝내기 전에 해당하는 것을 직접 실행해서 확인한다. "될 것이다"로 보고하지 않는다.

```bash
# 백엔드 테스트
cd api && ./gradlew test

# 전체 기동
docker compose up -d
curl localhost:8080/api/v1/health

# 핵심 동작
curl -X POST localhost:8080/api/v1/captures \
  -H "X-API-Token: $API_TOKEN" -H "Content-Type: application/json" \
  -d '{"text":"점심 9000원"}'
```

---

## 현재 진행 상황

> 태스크를 끝낼 때마다 이 줄을 갱신한다.

**진행 중:** T1.2 (Spring Boot 프로젝트 생성 + health)
**완료:** T1.1
