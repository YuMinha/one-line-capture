// 커서 페이징 상태만 다룬다. DOM을 모르므로 브라우저 없이 테스트할 수 있다.
// 탭을 바꿀 때 커서가 남아 있으면 다른 타입의 커서로 조회해 항목이 빠진다
export function createPager(fetchPage) {
  let type = null
  let cursor = null
  let hasNext = true
  let loading = false

  return {
    get type() { return type },
    get hasNext() { return hasNext },
    get loading() { return loading },

    reset(nextType) {
      type = nextType
      cursor = null
      hasNext = true
      loading = false
    },

    async next() {
      // 스크롤 이벤트는 연달아 들어온다. 이 가드가 없으면 같은 페이지를 두 번 붙인다
      if (loading || !hasNext) return []
      loading = true
      try {
        const page = await fetchPage({ type, cursor })
        cursor = page.nextCursor
        hasNext = page.hasNext
        return page.items
      } finally {
        loading = false
      }
    },
  }
}
