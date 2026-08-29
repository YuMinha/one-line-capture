import { describe as describeItem, fromKstInput, toKst, toKstInput, typeLabel } from '../src/format.js'
import assert from 'node:assert/strict'
import { test } from 'node:test'

test('지출은 금액에 천단위 콤마를 붙이고 항목을 아래에 둔다', () => {
  const result = describeItem({ type: 'EXPENSE', expense: { amount: 9000, merchant: '점심' } })
  assert.equal(result.main, '9,000원')
  assert.equal(result.sub, '점심')
})

test('항목이 없는 지출도 깨지지 않는다', () => {
  const result = describeItem({ type: 'EXPENSE', expense: { amount: 10000, merchant: null } })
  assert.equal(result.sub, '항목 없음')
})

test('마감 없는 할일은 마감 없음으로 표시한다', () => {
  const result = describeItem({ type: 'TODO', todo: { title: '우산 챙기기', dueAt: null } })
  assert.equal(result.main, '우산 챙기기')
  assert.equal(result.sub, '마감 없음')
})

test('마감은 UTC를 KST로 바꿔 보여준다', () => {
  // 2026-08-30T15:00Z = KST 2026-08-31 00:00
  const result = describeItem({ type: 'TODO', todo: { title: '과제', dueAt: '2026-08-30T15:00:00Z' } })
  assert.match(result.sub, /8\. 31\./)
  assert.match(result.sub, /00:00/)
})

test('링크는 메모를 앞세우고 없으면 URL을 쓴다', () => {
  assert.equal(describeItem({ link: { url: 'https://a.com', note: '정리글' } }).main, '정리글')
  assert.equal(describeItem({ link: { url: 'https://a.com', note: null } }).main, 'https://a.com')
})

test('상세가 없는 요약 항목은 원문을 보여준다', () => {
  const result = describeItem({ type: 'TODO', rawText: '우산 챙기기' })
  assert.equal(result.main, '우산 챙기기')
  assert.equal(result.sub, '')
})

test('타입 라벨은 한국어다', () => {
  assert.equal(typeLabel('EXPENSE'), '지출')
  assert.equal(typeLabel('TODO'), '할일')
  assert.equal(typeLabel('LINK'), '링크')
})

test('9시 UTC는 KST 18시다', () => {
  assert.match(toKst('2026-08-30T09:00:00Z'), /18:00/)
})

test('UTC를 datetime-local용 KST 문자열로 바꾼다', () => {
  assert.equal(toKstInput('2026-08-31T06:00:00Z'), '2026-08-31T15:00')
  assert.equal(toKstInput('2026-08-30T15:00:00Z'), '2026-08-31T00:00')
  assert.equal(toKstInput(null), '')
})

test('datetime-local 값은 KST로 해석해 UTC로 되돌린다', () => {
  assert.equal(fromKstInput('2026-08-31T15:00'), '2026-08-31T06:00:00.000Z')
  assert.equal(fromKstInput(''), null)
})

test('KST 변환은 왕복해도 값이 유지된다', () => {
  const iso = '2026-08-31T06:00:00.000Z'
  assert.equal(fromKstInput(toKstInput(iso)), iso)
})
