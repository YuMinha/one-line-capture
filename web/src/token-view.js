import { api } from './api.js'
import { getToken, setToken } from './token.js'
import { toast } from './toast.js'

// 가입 없이 이 기기 전용 계정을 받는다. 원문 토큰은 이 응답으로 한 번만 온다 (stack.md §5)
export async function startAsGuest() {
  const { token } = await api.issueGuest()
  setToken(token)
}

// 탐색 대상이 아니라 뒷문이라 화면 3개에 세지 않는다. 주인이 새 기기에서 자기 토큰을 넣을 때 쓴다 (spec.md §4)
export function renderToken(app, { onDone } = {}) {
  app.innerHTML = `
    <form id="token-form" autocomplete="off">
      <p class="lead">다른 기기에서 쓰던 토큰을 넣으면 그 기록을 이어서 봅니다. 주인이라면 서버의 <code>API_TOKEN</code> 값입니다.</p>
      <input id="token" type="password" placeholder="API 토큰" autocomplete="current-password" required />
      <button type="submit" class="primary">저장</button>
      <button type="button" id="token-new" class="ghost">새로 시작하기</button>
    </form>
  `

  const input = app.querySelector('#token')
  input.value = ''
  input.focus()

  app.querySelector('#token-form').addEventListener('submit', async (event) => {
    event.preventDefault()
    // 틀린 토큰으로 덮어쓰면 지금 쓰던 게스트 기록을 영영 못 연다. 실패하면 되돌린다
    const previous = getToken()
    setToken(input.value)
    try {
      // 저장하기 전에 실제로 통하는지 확인한다. 틀린 토큰을 저장해두면
      // 모든 화면이 조용히 비어 보인다
      await api.list({ size: 1 })
      toast('토큰을 저장했습니다', 'ok')
      onDone?.()
    } catch {
      if (previous) setToken(previous)
      toast('토큰이 올바르지 않습니다')
      input.focus()
      input.select()
    }
  })

  app.querySelector('#token-new').addEventListener('click', async () => {
    if (getToken() && !confirm('지금 기록은 이 기기에서 다시 열 수 없게 됩니다. 새로 시작할까요?')) return
    try {
      await startAsGuest()
      onDone?.()
    } catch (e) {
      toast(e.message)
    }
  })
}
