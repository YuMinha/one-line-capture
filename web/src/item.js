import { describe, typeLabel } from './format.js'

// onToggle을 주면 할일에는 체크박스, 링크에는 읽음 표시가 붙는다.
// 입력 화면의 미리보기처럼 토글이 필요 없는 곳에서는 주지 않는다
export function itemElement(item, { onToggle } = {}) {
  const { main, sub } = describe(item)

  const li = document.createElement('li')
  li.className = `item ${item.type.toLowerCase()}`
  li.dataset.id = item.id

  const badge = document.createElement('span')
  badge.className = 'badge'
  badge.textContent = typeLabel(item.type)

  const strong = document.createElement('strong')
  // 사용자가 던진 원문이 들어가는 자리다. innerHTML로 넣으면 그대로 실행된다
  strong.textContent = main

  const small = document.createElement('small')
  small.textContent = sub

  const body = document.createElement('div')
  body.className = 'item-body'
  body.append(strong, small)

  li.append(badge, body)

  const toggle = onToggle && toggleFor(item)
  if (toggle) {
    const box = document.createElement('input')
    box.type = 'checkbox'
    box.className = 'toggle'
    box.checked = toggle.checked
    box.title = toggle.title
    // 카드를 누르면 수정 시트가 열린다. 체크박스까지 시트를 열면 안 된다
    box.addEventListener('click', (event) => event.stopPropagation())
    box.addEventListener('change', () => onToggle(item, box.checked, box))
    li.append(box)
    li.classList.toggle('done', toggle.checked)
  }

  return li
}

function toggleFor(item) {
  if (item.todo) return { checked: item.todo.done, title: '완료' }
  if (item.link) return { checked: item.link.readAt != null, title: '읽음' }
  // 전체 탭은 상세가 없어 완료 여부를 모른다. 체크박스를 그리지 않는다
  return null
}
