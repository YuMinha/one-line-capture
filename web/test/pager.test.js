import { createPager } from '../src/pager.js'
import assert from 'node:assert/strict'
import { test } from 'node:test'

const pageOf = (ids, nextCursor) => ({
  items: ids.map((id) => ({ id })),
  nextCursor,
  hasNext: nextCursor != null,
})

test('첫 호출은 커서 없이, 다음 호출은 받은 커서로 조회한다', async () => {
  const calls = []
  const pager = createPager(async (query) => {
    calls.push({ ...query })
    return calls.length === 1 ? pageOf([5, 4], 4) : pageOf([3], null)
  })

  assert.deepEqual(await pager.next(), [{ id: 5 }, { id: 4 }])
  assert.deepEqual(await pager.next(), [{ id: 3 }])
  assert.deepEqual(calls, [{ type: null, cursor: null }, { type: null, cursor: 4 }])
})

test('마지막 페이지 뒤로는 더 부르지 않는다', async () => {
  let hits = 0
  const pager = createPager(async () => { hits++; return pageOf([1], null) })

  await pager.next()
  assert.equal(pager.hasNext, false)
  assert.deepEqual(await pager.next(), [])
  assert.equal(hits, 1)
})

test('탭을 바꾸면 커서가 초기화된다', async () => {
  const calls = []
  const pager = createPager(async (query) => { calls.push({ ...query }); return pageOf([9], 9) })

  await pager.next()
  pager.reset('EXPENSE')
  await pager.next()

  assert.deepEqual(calls[1], { type: 'EXPENSE', cursor: null })
})

test('앞선 요청이 끝나기 전에는 중복 요청하지 않는다', async () => {
  let hits = 0
  let release
  const pager = createPager(async () => {
    hits++
    await new Promise((resolve) => { release = resolve })
    return pageOf([1], 1)
  })

  const first = pager.next()
  const second = await pager.next()

  assert.deepEqual(second, [])
  assert.equal(hits, 1)
  release()
  await first
})

test('요청이 실패해도 잠금이 풀린다', async () => {
  let fail = true
  const pager = createPager(async () => {
    if (fail) { fail = false; throw new Error('네트워크') }
    return pageOf([1], null)
  })

  await assert.rejects(() => pager.next())
  assert.equal(pager.loading, false)
  assert.deepEqual(await pager.next(), [{ id: 1 }])
})
