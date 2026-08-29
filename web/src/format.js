// DOM을 모르는 순수 함수만 모은다. 이래야 브라우저 없이 테스트할 수 있다

const TYPE_LABEL = { EXPENSE: '지출', TODO: '할일', LINK: '링크' }

const won = new Intl.NumberFormat('ko-KR')

export function typeLabel(type) {
  return TYPE_LABEL[type] ?? type
}

// 서버는 UTC로 준다. 화면에 그릴 때만 KST로 바꾼다 (stack.md §2.2)
export function toKst(isoUtc) {
  return new Date(isoUtc).toLocaleString('ko-KR', {
    timeZone: 'Asia/Seoul',
    // h23을 명시하지 않으면 '오후 06:00'처럼 12시간제로 나온다
    hourCycle: 'h23',
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function describe(item) {
  if (item.expense) {
    return {
      main: `${won.format(item.expense.amount)}원`,
      sub: item.expense.merchant ?? '항목 없음',
    }
  }
  if (item.todo) {
    return {
      main: item.todo.title,
      sub: item.todo.dueAt ? `마감 ${toKst(item.todo.dueAt)}` : '마감 없음',
    }
  }
  if (item.link) {
    return {
      main: item.link.note ?? item.link.url,
      sub: item.link.url,
    }
  }
  // 타입이 섞인 전체 조회는 상세 없이 요약만 온다 (stack.md §2.5)
  return { main: item.rawText, sub: '' }
}

// datetime-local 입력은 타임존이 없는 벽시계 문자열이다. 브라우저 로컬 시각이 아니라
// 항상 KST로 해석한다 — 화면 표시(toKst)와 기준이 달라지면 사용자가 혼란스럽다.
// KST는 서머타임이 없어 고정 +09:00이라 이 계산이 안전하다
export function toKstInput(isoUtc) {
  if (!isoUtc) return ''
  // sv-SE 로케일은 'YYYY-MM-DD HH:mm:ss' 형태를 준다
  return new Date(isoUtc)
      .toLocaleString('sv-SE', { timeZone: 'Asia/Seoul' })
      .replace(' ', 'T')
      .slice(0, 16)
}

export function fromKstInput(value) {
  if (!value) return null
  return new Date(`${value}:00+09:00`).toISOString()
}
