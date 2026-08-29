import './style.css'
import { renderInput } from './input-view.js'
import { renderList } from './list-view.js'
import { renderSummary } from './summary-view.js'
import { renderToken } from './token-view.js'
import { clearToken, getToken } from './token.js'

const app = document.querySelector('#app')
const nav = document.querySelector('#nav')

// 해시 라우팅이라 폰에서 뒤로가기가 동작한다. 라우터 라이브러리는 화면 3개에 과하다
const ROUTES = {
  '#/list': renderList,
  '#/summary': renderSummary,
}

let cleanup = null

function route() {
  cleanup?.()

  // 토큰이 없으면 어떤 화면도 열지 않는다. 열어봐야 전부 401이다
  if (!getToken()) {
    cleanup = renderToken(app, { onDone: route }) ?? null
    nav.hidden = true
    return
  }
  nav.hidden = false

  const render = ROUTES[location.hash] ?? renderInput
  cleanup = render(app) ?? null
  for (const link of nav.children) {
    link.classList.toggle('on', link.getAttribute('href') === (location.hash || '#/'))
  }
}

window.addEventListener('hashchange', route)
route()

// 토큰이 만료되거나 서버에서 바뀌면 모든 요청이 401이 된다. 그때 토큰 화면으로 되돌린다
window.addEventListener('api:unauthorized', () => {
  clearToken()
  route()
})

// 서비스워커는 배포본에서만 등록한다. 개발 중에는 캐시가 HMR을 방해한다
if ('serviceWorker' in navigator && import.meta.env?.PROD) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // 등록 실패해도 앱은 그대로 동작한다. 홈 화면 설치만 안 될 뿐이다
    })
  })
}
