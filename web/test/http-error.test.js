import { describeHttpError } from '../src/http-error.js'
import assert from 'node:assert/strict'
import { test } from 'node:test'

test('서버가 준 메시지가 있으면 그대로 쓴다', () => {
  const result = describeHttpError(400, { error: { code: 'TEXT_REQUIRED', message: 'text는 비어 있을 수 없습니다' } })
  assert.deepEqual(result, { code: 'TEXT_REQUIRED', message: 'text는 비어 있을 수 없습니다' })
})

test('네트워크 단절(0)과 프록시 502는 같은 문장을 쓴다', () => {
  // 서버를 껐을 때 개발은 Vite가, 배포는 Caddy가 502를 준다.
  // 사용자에게는 "서버가 안 켜져 있다"는 같은 사실이다
  const offline = describeHttpError(0, null)
  assert.equal(offline.code, 'UNAVAILABLE')
  for (const status of [502, 503, 504]) {
    assert.equal(describeHttpError(status, null).message, offline.message)
  }
})

test('502에 본문이 없어도 알 수 없는 오류라고 하지 않는다', () => {
  assert.match(describeHttpError(502, null).message, /서버에 연결할 수 없습니다/)
})

test('404는 목록 새로고침을 안내한다', () => {
  assert.match(describeHttpError(404, null).message, /새로고침/)
})

test('그 밖의 5xx는 서버 문제로 안내한다', () => {
  assert.equal(describeHttpError(500, null).code, 'INTERNAL_ERROR')
})

test('본문이 깨져 있어도 죽지 않는다', () => {
  assert.equal(describeHttpError(400, { nope: 1 }).code, 'UNKNOWN')
  assert.equal(describeHttpError(400, undefined).code, 'UNKNOWN')
})
