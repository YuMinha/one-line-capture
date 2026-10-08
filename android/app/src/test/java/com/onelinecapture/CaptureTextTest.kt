package com.onelinecapture

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureTextTest {

    @Test
    fun `지출은 금액에 콤마와 원을 붙이고 항목을 뒤에 둔다`() {
        val json = JSONObject("""{"type":"EXPENSE","expense":{"amount":9000,"merchant":"점심","spentAt":"2026-10-09"}}""")
        assertEquals("지출로 저장됨 · 9,000원 점심", CaptureText.saved(json))
    }

    @Test
    fun `서버가 소수점 금액을 줘도 정수로 보여준다`() {
        val json = JSONObject("""{"type":"EXPENSE","expense":{"amount":15000.00,"merchant":null}}""")
        assertEquals("지출로 저장됨 · 15,000원", CaptureText.saved(json))
    }

    @Test
    fun `할일 마감은 UTC를 한국 시각으로 바꿔 보여준다`() {
        // 2026-10-07T06:00Z = KST 10/7 15:00
        val json = JSONObject("""{"type":"TODO","todo":{"title":"과제 제출","dueAt":"2026-10-07T06:00:00Z","done":false}}""")
        assertEquals("할일로 저장됨 · 과제 제출 (10/7 15:00)", CaptureText.saved(json))
    }

    @Test
    fun `마감 없는 할일은 제목만`() {
        val json = JSONObject("""{"type":"TODO","todo":{"title":"우산 챙기기","dueAt":null,"done":false}}""")
        assertEquals("할일로 저장됨 · 우산 챙기기", CaptureText.saved(json))
    }

    @Test
    fun `링크는 메모를 앞세우고 없으면 URL`() {
        val withNote = JSONObject("""{"type":"LINK","link":{"url":"https://a.com","note":"정리글"}}""")
        val noNote = JSONObject("""{"type":"LINK","link":{"url":"https://a.com","note":null}}""")
        assertEquals("링크로 저장됨 · 정리글", CaptureText.saved(withNote))
        assertEquals("링크로 저장됨 · https://a.com", CaptureText.saved(noNote))
    }

    @Test
    fun `서버 에러 메시지를 그대로 쓴다`() {
        val body = """{"error":{"code":"LINK_CODE_INVALID","message":"코드가 틀렸거나 만료됐습니다"}}"""
        assertEquals("코드가 틀렸거나 만료됐습니다", CaptureText.errorMessage(400, body))
    }

    @Test
    fun `본문이 없으면 상태 코드로 문구를 고른다`() {
        assertEquals("서버에 연결할 수 없습니다. 인터넷을 확인해 주세요", CaptureText.errorMessage(0, null))
        assertEquals("서버에 문제가 생겼습니다. 잠시 뒤에 다시 해 주세요", CaptureText.errorMessage(502, "<html>"))
    }

    @Test
    fun `필터의 UNAUTHORIZED만 토큰이 죽은 것으로 본다`() {
        assertEquals(true, ApiException(401, "UNAUTHORIZED", "").tokenRejected)
        assertEquals(false, ApiException(401, "LOGIN_FAILED", "").tokenRejected)
        assertEquals(false, ApiException(400, "LINK_CODE_INVALID", "").tokenRejected)
    }
}
