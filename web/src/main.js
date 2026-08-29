import './style.css'
import { api, ApiError } from './api.js'
import { describe, typeLabel } from './format.js'

const RECENT_LIMIT = 3

const app = document.querySelector('#app')

app.innerHTML = `
  <h1>한 줄 캡처</h1>
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

function setStatus(text, kind = '') {
  status.textContent = text
  status.className = `status ${kind}`
}

function renderRecent() {
  recentList.replaceChildren(...recent.map((item) => {
    const { main, sub } = describe(item)
    const li = document.createElement('li')
    li.className = `item ${item.type.toLowerCase()}`
    // textContent로만 넣는다. innerHTML에 사용자 입력을 넣으면 그대로 실행된다
    const badge = document.createElement('span')
    badge.className = 'badge'
    badge.textContent = typeLabel(item.type)
    const body = document.createElement('div')
    const strong = document.createElement('strong')
    strong.textContent = main
    const small = document.createElement('small')
    small.textContent = sub
    body.append(strong, small)
    li.append(badge, body)
    return li
  }))
}

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
  if (text) {
    save(text)
  }
})

// 새로고침해도 방금 뭘 남겼는지 보이도록 최근 것을 먼저 가져온다
api.list({ size: RECENT_LIMIT })
    .then((page) => {
      recent = page.items
      renderRecent()
    })
    .catch(() => setStatus('서버에 연결할 수 없습니다', 'fail'))
