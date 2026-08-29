import { api } from './api.js'
import { setToken } from './token.js'
import { toast } from './toast.js'

// 탐색 대상이 아니라 일회성 관문이라 화면 3개에 세지 않는다 (spec.md §4)
export function renderToken(app, { onDone } = {}) {
  app.innerHTML = `
    <form id="token-form" autocomplete="off">
      <p class="lead">이 앱은 토큰 하나로 내 기록을 지킨다. 서버의 <code>API_TOKEN</code> 값을 넣어 주세요.</p>
      <input id="token" type="password" placeholder="API 토큰" autocomplete="current-password" required />
      <button type="submit" class="primary">저장</button>
    </form>
  `

  const input = app.querySelector('#token')
  input.value = ''
  input.focus()

  app.querySelector('#token-form').addEventListener('submit', async (event) => {
    event.preventDefault()
    setToken(input.value)
    try {
      // 저장하기 전에 실제로 통하는지 확인한다. 틀린 토큰을 저장해두면
      // 모든 화면이 조용히 비어 보인다
      await api.list({ size: 1 })
      toast('토큰을 저장했습니다', 'ok')
      onDone?.()
    } catch {
      toast('토큰이 올바르지 않습니다')
      input.focus()
      input.select()
    }
  })
}
