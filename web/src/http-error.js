// 상태 코드를 사람이 읽을 문장으로 바꾼다. 순수 함수라 브라우저 없이 테스트한다.
// 서버가 죽으면 리버스 프록시(개발은 Vite, 배포는 Caddy)가 502를 준다.
// fetch는 성공하므로 네트워크 단절과 다른 경로로 들어온다 — 사용자에겐 같은 일이다
const UNAVAILABLE = '서버에 연결할 수 없습니다. 잠시 뒤 다시 시도해 주세요.'

export function describeHttpError(status, body) {
  const fromServer = body?.error
  if (fromServer?.message) {
    return { code: fromServer.code ?? 'UNKNOWN', message: fromServer.message }
  }
  if (status === 0 || status === 502 || status === 503 || status === 504) {
    return { code: 'UNAVAILABLE', message: UNAVAILABLE }
  }
  if (status === 404) {
    return { code: 'NOT_FOUND', message: '없는 항목입니다. 목록을 새로고침해 주세요.' }
  }
  if (status >= 500) {
    return { code: 'INTERNAL_ERROR', message: '서버에서 문제가 발생했습니다.' }
  }
  return { code: 'UNKNOWN', message: '요청을 처리하지 못했습니다.' }
}
