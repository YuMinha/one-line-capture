import { api, ApiError } from './api.js'
import { itemElement } from './item.js'
import { typeLabel } from './format.js'

const RECENT_LIMIT = 3

export function renderInput(app) {
  app.innerHTML = `
    <form id="form" autocomplete="off">
      <input id="input" type="text" maxlength="500" placeholder="점심 9000원" enterkeyhint="done" autofocus />
    </form>
    <p id="status" class="status" role="status" aria-live="polite"></p>
    <ul id="recent" class="items"></ul>
  `

  const form = app.querySelector('#form')
  const input = app.querySelector('#input')
  const status = app.querySelector('#status')
  const recentList = app.querySelector('#recent')

  let recent = []

  const setStatus = (text, kind = '') => {
    status.textContent = text
    status.className = `status ${kind}`
  }

  const renderRecent = () => recentList.replaceChildren(...recent.map(itemElement))

  async function save(text) {
    // 화면을 바꾸지 않는 게 이 앱의 핵심이다 (spec.md §3).
    // 입력창을 먼저 비워 다음 줄을 바로 칠 수 있게 하고, 실패하면 되돌린다
    input.value = ''
    setStatus('저장 중...')
    try {
      const saved = await api.create(text)
      recent = [saved, ...recent].slice(0, RECENT_LIMIT)
      renderRecent()
      setStatus(`${typeLabel(saved.type)}으로 저장됨`, 'ok')
    } catch (error) {
      // 사용자가 친 글자를 잃지 않는 게 어떤 에러 메시지보다 중요하다
      input.value = text
      setStatus(error instanceof ApiError ? error.message : '저장하지 못했습니다', 'fail')
    } finally {
      input.focus()
    }
  }

  form.addEventListener('submit', (event) => {
    event.preventDefault()
    const text = input.value.trim()
    if (text) save(text)
  })

  api.list({ size: RECENT_LIMIT })
      .then((page) => { recent = page.items; renderRecent() })
      .catch(() => setStatus('서버에 연결할 수 없습니다', 'fail'))
}
