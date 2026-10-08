package com.onelinecapture

import org.json.JSONObject
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// 서버 응답을 사람이 읽을 한 줄로 바꾼다. 안드로이드 API를 모르게 둬야 기기 없이 테스트할 수 있다
object CaptureText {

    // 서버는 UTC로 준다. 사람은 한국 시각으로 읽는다 (stack.md §2.2)
    private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private val DUE = DateTimeFormatter.ofPattern("M/d HH:mm")

    fun label(type: String): String = when (type) {
        "EXPENSE" -> "지출"
        "TODO" -> "할일"
        "LINK" -> "링크"
        else -> type
    }

    // 세 라벨 모두 받침이 없거나 ㄹ 받침이라 '으로'가 아니라 '로'다 (웹 format.js와 같은 규칙)
    fun saved(capture: JSONObject): String {
        val type = capture.optString("type")
        val head = "${label(type)}로 저장됨"
        val detail = detail(capture, type)
        return if (detail.isNullOrBlank()) head else "$head · $detail"
    }

    private fun detail(capture: JSONObject, type: String): String? = when (type) {
        "EXPENSE" -> capture.optJSONObject("expense")?.let { e ->
            val amount = won(e.opt("amount"))
            val merchant = e.optStringOrNull("merchant")
            listOfNotNull(amount, merchant).joinToString(" ")
        }
        "TODO" -> capture.optJSONObject("todo")?.let { t ->
            val title = t.optStringOrNull("title")
            val due = t.optStringOrNull("dueAt")?.let { DUE.format(Instant.parse(it).atZone(KST)) }
            if (due == null) title else "$title ($due)"
        }
        "LINK" -> capture.optJSONObject("link")?.let { l -> l.optStringOrNull("note") ?: l.optStringOrNull("url") }
        else -> null
    }

    private fun won(raw: Any?): String? {
        if (raw == null || raw == JSONObject.NULL) return null
        val amount = BigDecimal(raw.toString())
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(amount) + "원"
    }

    // 서버 에러는 { error: { code, message } } 한 모양이다. 그 메시지를 그대로 보여준다
    fun errorMessage(status: Int, body: String?): String {
        val fromServer = runCatching { JSONObject(body ?: "").getJSONObject("error").getString("message") }.getOrNull()
        if (!fromServer.isNullOrBlank()) return fromServer
        return when {
            status == 0 -> "서버에 연결할 수 없습니다. 인터넷을 확인해 주세요"
            status >= 500 -> "서버에 문제가 생겼습니다. 잠시 뒤에 다시 해 주세요"
            else -> "요청을 처리하지 못했습니다 ($status)"
        }
    }

    // optString은 null을 "null" 문자열로 돌려준다. 그게 화면에 찍히면 안 된다
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
