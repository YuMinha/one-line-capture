const KEY = 'capture.apiToken'

// localStorage에 둔다. XSS가 있으면 털리지만 외부 스크립트를 붙이지 않으므로 감수한다.
// 게스트에게는 이 값이 기록을 여는 유일한 열쇠다. 지워지면 서버도 되살려줄 수 없다 (spec.md §7)
export function getToken() {
  try {
    return localStorage.getItem(KEY) ?? ''
  } catch {
    // 사생활 보호 모드 등에서 접근 자체가 막힐 수 있다
    return ''
  }
}

export function setToken(token) {
  try {
    localStorage.setItem(KEY, token.trim())
  } catch {
    // 저장을 못 해도 이번 세션은 동작해야 한다
  }
}

export function clearToken() {
  try {
    localStorage.removeItem(KEY)
  } catch {
    /* 무시 */
  }
}
