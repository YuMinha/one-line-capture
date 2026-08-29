import { api, ApiError } from './api.js'
import { createPager } from './pager.js'
import { itemElement } from './item.js'

const TABS = [
  { label: '전체', type: null },
  { label: '지출', type: 'EXPENSE' },
  { label: '할일', type: 'TODO' },
  { label: '링크', type: 'LINK' },
]

export function renderList(app) {
  app.innerHTML = `
    <div id="tabs" class="tabs" role="tablist">
      ${TABS.map((tab, i) =>
        `<button role="tab" data-type="${tab.type ?? ''}" class="${i === 0 ? 'on' : ''}">${tab.label}</button>`,
      ).join('')}
    </div>
    <ul id="list" class="items"></ul>
    <p id="status" class="status" role="status" aria-live="polite"></p>
    <div id="sentinel"></div>
  `

  const tabs = app.querySelector('#tabs')
  const list = app.querySelector('#list')
  const status = app.querySelector('#status')
  const sentinel = app.querySelector('#sentinel')

  const pager = createPager((query) => api.list({ ...query, size: 20 }))

  async function loadMore() {
    if (!pager.hasNext || pager.loading) return
    try {
      const items = await pager.next()
      list.append(...items.map(itemElement))
      status.className = 'status'
      status.textContent = pager.hasNext ? '' : (list.children.length ? '' : '아직 아무것도 없습니다')
    } catch (error) {
      status.className = 'status fail'
      status.textContent = error instanceof ApiError ? error.message : '불러오지 못했습니다'
    }
  }

  function selectTab(type) {
    for (const button of tabs.children) {
      button.classList.toggle('on', (button.dataset.type || null) === type)
    }
    pager.reset(type)
    list.replaceChildren()
    status.textContent = ''
    loadMore()
  }

  tabs.addEventListener('click', (event) => {
    const button = event.target.closest('button')
    if (button) selectTab(button.dataset.type || null)
  })

  // 스크롤 이벤트를 직접 듣지 않는다. 브라우저가 주는 감시자가 더 정확하고 싸다.
  // rootMargin으로 바닥에 닿기 전에 미리 불러온다
  const observer = new IntersectionObserver(
    (entries) => { if (entries[0].isIntersecting) loadMore() },
    { rootMargin: '200px' },
  )
  observer.observe(sentinel)

  selectTab(null)

  // 화면을 떠날 때 감시자를 끊지 않으면 목록이 사라진 뒤에도 계속 불러온다
  return () => observer.disconnect()
}
