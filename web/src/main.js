import './style.css'
import { renderInput } from './input-view.js'
import { renderList } from './list-view.js'
import { renderSummary } from './summary-view.js'

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
  const render = ROUTES[location.hash] ?? renderInput
  cleanup = render(app) ?? null
  for (const link of nav.children) {
    link.classList.toggle('on', link.getAttribute('href') === (location.hash || '#/'))
  }
}

window.addEventListener('hashchange', route)
route()
