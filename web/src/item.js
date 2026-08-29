import { describe, typeLabel } from './format.js'

// 사용자가 던진 원문이 들어가는 자리다. innerHTML로 넣으면 그대로 실행된다
export function itemElement(item) {
  const { main, sub } = describe(item)

  const li = document.createElement('li')
  li.className = `item ${item.type.toLowerCase()}`
  li.dataset.id = item.id

  const badge = document.createElement('span')
  badge.className = 'badge'
  badge.textContent = typeLabel(item.type)

  const strong = document.createElement('strong')
  strong.textContent = main

  const small = document.createElement('small')
  small.textContent = sub

  const body = document.createElement('div')
  body.className = 'item-body'
  body.append(strong, small)

  li.append(badge, body)
  return li
}
