// 기본값이 상대경로인 게 중요하다. 개발에서는 Vite 프록시가, 배포에서는 Caddy가
// 같은 출처로 넘겨주므로 CORS가 필요 없다 (stack.md §6)
import { describeHttpError } from './http-error.js'
import { getToken } from './token.js'

const BASE_URL = import.meta.env?.VITE_API_BASE_URL ?? '/api/v1'

export class ApiError extends Error {
  constructor(status, code, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(BASE_URL + path, {
      ...options,
      // 헤더 주입 지점을 여기 한 곳으로 모아둔 이유가 이것이다. 호출부는 토큰을 모른다
      headers: { 'Content-Type': 'application/json', 'X-API-Token': getToken(), ...options.headers },
    })
  } catch {
    // fetch는 네트워크가 끊겼을 때만 reject한다. 404나 500은 여기로 안 온다
    const { code, message } = describeHttpError(0, null)
    throw new ApiError(0, code, message)
  }

  if (response.status === 204) return null

  const body = await response.json().catch(() => null)

  if (!response.ok) {
    if (response.status === 401) {
      // 라우터가 토큰 화면으로 되돌린다. api.js는 화면을 모른다
      window.dispatchEvent(new CustomEvent('api:unauthorized'))
    }
    const { code, message } = describeHttpError(response.status, body)
    throw new ApiError(response.status, code, message)
  }
  return body
}

const json = (body) => ({ method: 'POST', body: JSON.stringify(body) })

export const api = {
  health: () => request('/health'),

  get: (id) => request(`/captures/${id}`),

  create: (text) => request('/captures', json({ text })),

  update: (id, body) => request(`/captures/${id}`, { method: 'PATCH', body: JSON.stringify(body) }),

  remove: (id) => request(`/captures/${id}`, { method: 'DELETE' }),

  // 뒤집기가 아니라 원하는 값을 보낸다. 재시도해도 결과가 같다
  setDone: (id, value) => request(`/todos/${id}`, { method: 'PATCH', body: JSON.stringify({ value }) }),

  setRead: (id, value) => request(`/links/${id}`, { method: 'PATCH', body: JSON.stringify({ value }) }),

  preview: (text) => request('/captures/preview', json({ text })),

  expenseSummary: (month) => request(`/summary/expenses${month ? `?month=${month}` : ''}`),

  list: ({ type, cursor, size, done } = {}) => {
    const params = new URLSearchParams()
    if (type) params.set('type', type)
    if (cursor != null) params.set('cursor', cursor)
    if (size != null) params.set('size', size)
    if (done != null) params.set('done', done)
    const query = params.toString()
    return request('/captures' + (query ? `?${query}` : ''))
  },
}
