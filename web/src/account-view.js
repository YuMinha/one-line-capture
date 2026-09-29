import { api } from './api.js'
import { escapeHtml } from './escape-html.js'
import { clearToken, setToken } from './token.js'
import { toast, toastError } from './toast.js'

// 탐색 대상이 아니라 가끔 들르는 설정이라 화면 3개에 세지 않는다 (spec.md §4). 푸터 링크로만 들어온다
export function renderAccount(app, { onSwitched } = {}) {
  let alive = true
  let timer = null

  app.innerHTML = '<p class="lead">불러오는 중...</p>'
  api.me()
    .then((me) => {
      // 응답이 오기 전에 다른 탭으로 갔으면 그 화면을 덮어쓰지 않는다
      if (alive) me.registered ? renderMember(me) : renderGuest()
    })
    .catch(toastError)

  function renderGuest() {
    app.innerHTML = `
      <section class="account">
        <h2>계정 만들기</h2>
        <p class="lead">지금까지 이 기기에서 쓴 기록이 그대로 계정에 남습니다.</p>
        <form id="register" class="stack" autocomplete="on">
          <input name="loginId" placeholder="아이디 (영문 소문자·숫자·_ 4~20자)" autocomplete="username" autocapitalize="none" spellcheck="false" required />
          <input name="email" type="email" placeholder="이메일 (비밀번호 찾기용)" autocomplete="email" required />
          <input name="password" type="password" placeholder="비밀번호 (8자 이상)" autocomplete="new-password" minlength="8" required />
          <button class="primary">가입</button>
        </form>
      </section>
      <section class="account">
        <h2>이미 계정이 있어요</h2>
        <p class="lead">로그인하면 이 기기에서 게스트로 쓴 기록은 더 이상 보이지 않습니다. 남기고 싶으면 위에서 먼저 가입하세요.</p>
        <form id="login" class="stack">
          <input name="loginId" placeholder="아이디" autocomplete="username" autocapitalize="none" spellcheck="false" required />
          <input name="password" type="password" placeholder="비밀번호" autocomplete="current-password" required />
          <button class="primary">로그인</button>
        </form>
        <form id="link" class="stack">
          <input name="code" placeholder="연결 코드 (예: K7QM-3XPD)" autocomplete="one-time-code" autocapitalize="characters" spellcheck="false" required />
          <button class="ghost">코드로 연결</button>
        </form>
      </section>
      ${codeSection()}
      <p class="lead small"><a href="#/token">토큰 직접 입력</a></p>
    `
    onSubmit('#register', async (form) => {
      const me = await api.register(values(form))
      toast('가입했습니다', 'ok')
      renderMember(me)
    })
    onSubmit('#login', async (form) => switchTo((await api.login(values(form))).token))
    onSubmit('#link', async (form) => switchTo((await api.link(values(form).code)).token))
    bindCode()
  }

  function renderMember(me) {
    app.innerHTML = `
      <section class="account">
        <h2>${escapeHtml(me.loginId)}</h2>
        <p class="lead">${escapeHtml(me.email)}</p>
      </section>
      ${codeSection()}
      <section class="account">
        <button id="logout" class="ghost">이 기기에서 로그아웃</button>
      </section>
    `
    bindCode()
    app.querySelector('#logout').addEventListener('click', async () => {
      // 서버에서 이미 끊겼어도 이 기기에서는 지운다. 로그아웃이 실패해서 못 나가는 일은 없어야 한다
      await api.logout().catch(() => {})
      clearToken()
      toast('로그아웃했습니다', 'ok')
      onSwitched?.()
    })
  }

  function codeSection() {
    return `
      <section class="account">
        <h2>다른 기기 연결</h2>
        <p class="lead">새 기기의 계정 화면에서 이 코드를 넣으면 비밀번호 없이 같은 기록을 봅니다. 5분 동안 한 번만 쓸 수 있습니다.</p>
        <button id="code-btn" class="ghost">코드 받기</button>
        <p id="code" class="link-code" hidden></p>
      </section>
    `
  }

  function bindCode() {
    const button = app.querySelector('#code-btn')
    const out = app.querySelector('#code')
    button.addEventListener('click', async () => {
      try {
        const { code, expiresAt } = await api.linkCode()
        clearInterval(timer)
        const tick = () => {
          const left = Math.max(0, Math.round((Date.parse(expiresAt) - Date.now()) / 1000))
          if (left === 0) {
            clearInterval(timer)
            out.hidden = true
            button.textContent = '새 코드 받기'
            return
          }
          const mm = Math.floor(left / 60)
          const ss = String(left % 60).padStart(2, '0')
          out.innerHTML = `<strong>${escapeHtml(code)}</strong><span>${mm}:${ss}</span>`
        }
        tick()
        timer = setInterval(tick, 1000)
        out.hidden = false
        button.textContent = '새 코드 받기'
      } catch (error) {
        toastError(error)
      }
    })
  }

  function switchTo(token) {
    setToken(token)
    toast('연결했습니다', 'ok')
    onSwitched?.()
  }

  return () => {
    alive = false
    clearInterval(timer)
  }
}

function values(form) {
  return Object.fromEntries(new FormData(form))
}

// 제출 중에는 버튼을 막는다. 두 번 눌러 가입이 두 번 나가면 두 번째가 409로 보인다
function onSubmit(selector, handler) {
  const form = document.querySelector(selector)
  form.addEventListener('submit', async (event) => {
    event.preventDefault()
    const button = form.querySelector('button')
    button.disabled = true
    try {
      await handler(form)
    } catch (error) {
      toastError(error)
    } finally {
      button.disabled = false
    }
  })
}
