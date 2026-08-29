import { escapeHtml } from '../src/escape-html.js'
import assert from 'node:assert/strict'
import { test } from 'node:test'

test('스크립트 태그가 태그로 남지 않는다', () => {
  const result = escapeHtml('<script>alert(1)</script>')
  assert.equal(result, '&lt;script&gt;alert(1)&lt;/script&gt;')
  assert.ok(!result.includes('<script'))
})

test('큰따옴표 속성값을 탈출하지 못한다', () => {
  // value="..." 안에 들어가므로 따옴표를 닫고 onerror를 붙이는 공격을 막아야 한다
  const payload = '" onerror="alert(1)'
  const result = escapeHtml(payload)
  assert.ok(!result.includes('"'))
  assert.equal(result, '&quot; onerror=&quot;alert(1)')
})

test('img onerror 페이로드', () => {
  const result = escapeHtml('<img src=x onerror=alert(1)>')
  assert.ok(!result.includes('<img'))
})

test('앰퍼샌드를 먼저 바꾸므로 이중 인코딩 우회가 안 된다', () => {
  // &lt;script&gt; 를 넣어 &가 살아남으면 브라우저가 다시 디코딩해 태그가 된다
  assert.equal(escapeHtml('&lt;script&gt;'), '&amp;lt;script&amp;gt;')
})

test('null과 undefined는 빈 문자열이다', () => {
  assert.equal(escapeHtml(null), '')
  assert.equal(escapeHtml(undefined), '')
})

test('숫자와 평범한 한글은 그대로 남는다', () => {
  assert.equal(escapeHtml(9000), '9000')
  assert.equal(escapeHtml('점심 9000원'), '점심 9000원')
})
