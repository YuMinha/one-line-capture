// 기본값이 상대경로인 게 중요하다. 개발에서는 Vite 프록시가, 배포에서는 Caddy가
// 같은 출처로 넘겨주므로 CORS가 필요 없다 (stack.md §6)
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

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
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    // fetch는 네트워크가 끊겼을 때만 reject한다. 404나 500은 여기로 안 온다
    throw new ApiError(0, 'NETWORK', '서버에 연결할 수 없습니다')
  }

  if (response.status === 204) return null

  const body = await response.json().catch(() => null)

  if (!response.ok) {
    throw new ApiError(
      response.status,
      body?.error?.code ?? 'UNKNOWN',
      body?.error?.message ?? '알 수 없는 오류가 발생했습니다',
    )
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

  preview: (text) => request('/captures/preview', json({ text })),

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
