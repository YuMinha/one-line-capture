// innerHTML 템플릿에 사용자 값을 넣을 때 쓴다. 큰따옴표 속성값까지 안전하도록
// & < > " 를 모두 바꾼다. 작은따옴표 속성은 이 프로젝트에서 쓰지 않는다
const MAP = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }

export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"]/g, (c) => MAP[c])
}
