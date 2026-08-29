import './style.css'
import { api, ApiError } from './api.js'

const app = document.querySelector('#app')

// T3.2에서 입력 화면으로 바뀐다. 지금은 API가 닿는지만 확인한다
async function showHealth() {
  app.innerHTML = '<p class="status">서버 확인 중...</p>'
  try {
    const result = await api.health()
    app.innerHTML = `<p class="status ok">서버 정상 · status=${result.status}</p>`
  } catch (error) {
    const message = error instanceof ApiError ? error.message : String(error)
    app.innerHTML = `<p class="status fail">${message}</p>`
  }
}

showHealth()
