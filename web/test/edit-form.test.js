import { FORMS } from '../src/edit-form.js'
import assert from 'node:assert/strict'
import { test } from 'node:test'

test('지출 폼은 현재 값을 채우고, 비어 있으면 안전한 기본값을 쓴다', () => {
  const filled = FORMS.EXPENSE.fields({ expense: { amount: 9000, merchant: '점심', spentAt: '2026-08-30' } })
  assert.deepEqual(filled.map((f) => f.value), [9000, '점심', '2026-08-30'])

  const empty = FORMS.EXPENSE.fields({ expense: null })
  assert.deepEqual(empty.map((f) => f.value), [0, '', ''])
})

test('타입을 바꾸면 상세가 없어도 폼이 그려진다', () => {
  // 지출을 할일로 바꾸는 순간 item.todo는 없다. 제목은 원문으로 채운다
  const fields = FORMS.TODO.fields({ rawText: '점심 9000원', expense: { amount: 9000 } })
  assert.equal(fields[0].value, '점심 9000원')
  assert.equal(fields[1].value, '')
})

test('PATCH 본문은 모든 필드를 채운다 - 빠뜨리면 서버가 null로 덮는다', () => {
  const body = FORMS.EXPENSE.body({ amount: '12000', merchant: '저녁', spentAt: '2026-08-31' })
  assert.deepEqual(body, { expense: { amount: 12000, merchant: '저녁', spentAt: '2026-08-31' } })
})

test('금액은 문자열이 아니라 숫자로 보낸다', () => {
  assert.equal(typeof FORMS.EXPENSE.body({ amount: '9000', spentAt: '2026-08-30' }).expense.amount, 'number')
})

test('빈 선택 입력은 빈 문자열이 아니라 null로 보낸다', () => {
  assert.equal(FORMS.EXPENSE.body({ amount: '1', merchant: '', spentAt: '2026-08-30' }).expense.merchant, null)
  assert.equal(FORMS.LINK.body({ url: 'https://a.com', note: '' }).link.note, null)
})

test('마감은 KST 입력을 UTC로 바꿔 보낸다', () => {
  const body = FORMS.TODO.body({ title: '과제', dueAt: '2026-08-31T15:00' })
  assert.deepEqual(body, { todo: { title: '과제', dueAt: '2026-08-31T06:00:00.000Z' } })
})

test('마감을 비우면 null이 간다', () => {
  assert.equal(FORMS.TODO.body({ title: '과제', dueAt: '' }).todo.dueAt, null)
})
