const KEY = 'capture.apiToken'

// localStorage에 둔다. XSS가 있으면 털리지만 v1은 사용자가 나 하나고 외부 스크립트를
// 붙이지 않으므로 감수한다. v2에서 다중 사용자가 되면 HttpOnly 쿠키로 바뀐다 (stack.md §5)
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
