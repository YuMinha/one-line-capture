import { ApiError } from './api.js'

const DURATION = 4000

let container = null

function ensureContainer() {
  if (!container) {
    container = document.createElement('div')
    container.className = 'toasts'
    // 스크린리더가 읽도록. 에러는 눈으로만 보이면 안 된다
    container.setAttribute('role', 'status')
    container.setAttribute('aria-live', 'polite')
    document.body.append(container)
  }
  return container
}

export function toast(message, kind = 'fail') {
  const el = document.createElement('div')
  el.className = `toast ${kind}`
  el.textContent = message
  ensureContainer().append(el)
  setTimeout(() => el.remove(), DURATION)
}

// 문장 만들기는 http-error.js가 이미 했다. 여기서는 띄우기만 한다
export function toastError(error, fallback = '문제가 발생했습니다') {
  toast(error instanceof ApiError ? error.message : fallback)
}
