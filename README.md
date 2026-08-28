# 한 줄 캡처

입력창에 한 줄만 던지면 **지출 / 할일 / 링크**로 자동 분류해 저장하는 개인 기록 앱.

```
점심 9000원          → 지출  9,000원 · 점심
내일 3시 과제 제출   → 할일  마감 내일 15:00
https://... 스프링   → 링크  URL + 메모
```

## 스택

Java 21 · Spring Boot 3 · MySQL 8 · Flyway · 바닐라 JS + Vite · Docker Compose

## 로컬 실행

> 아직 구현 전. T1.8에서 `docker compose up` 하나로 뜨게 만든다.

```bash
cp .env.example .env   # 값을 채운다
docker compose up
```

## API

> T4.8에서 채운다. 초안은 `docs/stack.md` §3.

| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/api/v1/health` | 헬스체크 |
| `POST` | `/api/v1/captures` | 한 줄 저장 |

## 문서

- [`docs/spec.md`](docs/spec.md) — 무엇을 만들고 무엇을 안 만드는가
- [`docs/stack.md`](docs/stack.md) — 스택 근거, DDL, API 스펙, 파서 설계
- [`docs/tasks.md`](docs/tasks.md) — 주차별 작업 목록

## 라이선스

MIT — [`LICENSE`](LICENSE)
