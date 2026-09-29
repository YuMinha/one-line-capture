import './style.css'
import { renderAccount } from './account-view.js'
import { renderInput } from './input-view.js'
import { renderList } from './list-view.js'
import { renderSummary } from './summary-view.js'
import { toast } from './toast.js'
import { renderToken, startAsGuest } from './token-view.js'
import { clearToken, getToken } from './token.js'

const app = document.querySelector('#app')
const nav = document.querySelector('#nav')

// 해시 라우팅이라 폰에서 뒤로가기가 동작한다. 라우터 라이브러리는 화면 3개에 과하다
const ROUTES = {
  '#/list': renderList,
  '#/summary': renderSummary,
  // 로그인·연결로 토큰이 바뀌면 입력 화면으로 보낸다. 다른 계정의 화면이 남아 있으면 안 된다
  '#/account': (app) => renderAccount(app, { onSwitched: goHome }),
}

let cleanup = null

const goHome = () => {
  if (location.hash === '#/' || location.hash === '') route()
  else location.hash = '#/'
}

async function route() {
  cleanup?.()
  cleanup = null

  if (location.hash === '#/token') {
    cleanup = renderToken(app, { onDone: goHome }) ?? null
    markNav()
    return
  }

  // 처음 온 기기다. 입력할 것 없이 바로 쓰게 이 기기 전용 계정을 받는다 (spec.md §2)
  if (!getToken()) {
    try {
      await startAsGuest()
    } catch (e) {
      // 발급이 안 되면 어떤 화면을 열어도 전부 401이다. 다시 시도할 수 있는 곳으로 보낸다
      toast(e.message)
      location.hash = '#/token'
      return
    }
  }

  const render = ROUTES[location.hash] ?? renderInput
  cleanup = render(app) ?? null
  markNav()
}

function markNav() {
  for (const link of nav.children) {
    link.classList.toggle('on', link.getAttribute('href') === (location.hash || '#/'))
  }
}

window.addEventListener('hashchange', route)
route()

// 주인 토큰이 바뀌었거나 게스트 계정이 사라지면 모든 요청이 401이 된다.
// 여기서 자동으로 새 게스트를 받지 않는다. 새로 받은 토큰도 401이면 발급과 401이 끝없이 돈다.
// 토큰 화면에서 사람이 "토큰 넣기"와 "새로 시작" 중에 고르게 한다
window.addEventListener('api:unauthorized', () => {
  clearToken()
  if (location.hash !== '#/token') location.hash = '#/token'
})

// 서비스워커는 배포본에서만 등록한다. 개발 중에는 캐시가 HMR을 방해한다
if ('serviceWorker' in navigator && import.meta.env?.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // 등록 실패해도 앱은 그대로 동작한다. 홈 화면 설치만 안 될 뿐이다
    })
  })
}
