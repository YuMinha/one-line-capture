package com.onelinecapture

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApiException(val status: Int, val code: String?, message: String) : Exception(message) {
    // 필터가 보내는 UNAUTHORIZED일 때만 토큰이 죽은 것이다. 연결 코드가 틀린 400과 섞으면 안 된다
    val tokenRejected: Boolean get() = status == 401 && code == "UNAUTHORIZED"
}

// 앱은 입력 전용이라 부르는 API가 셋뿐이다. HTTP 라이브러리를 들이지 않는다 (stack.md §8)
object CaptureApi {

    private const val TIMEOUT_MS = 10_000

    fun link(code: String): String =
        request("POST", "/api/v1/auth/link", null, JSONObject().put("code", code))!!.getString("token")

    fun create(token: String, text: String): JSONObject =
        request("POST", "/api/v1/captures", token, JSONObject().put("text", text))!!

    fun logout(token: String) {
        request("POST", "/api/v1/auth/logout", token, null)
    }

    private fun request(method: String, path: String, token: String?, body: JSONObject?): JSONObject? {
        val connection = try {
            (URL(BuildConfig.BASE_URL + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                if (token != null) setRequestProperty("X-API-Token", token)
                if (body != null) {
                    doOutput = true
                    outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
            }
        } catch (e: IOException) {
            throw ApiException(0, null, CaptureText.errorMessage(0, null))
        }

        try {
            val status = try {
                connection.responseCode
            } catch (e: IOException) {
                throw ApiException(0, null, CaptureText.errorMessage(0, null))
            }
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            if (status !in 200..299) {
                val code = runCatching { JSONObject(text ?: "").getJSONObject("error").getString("code") }.getOrNull()
                throw ApiException(status, code, CaptureText.errorMessage(status, text))
            }
            return if (text.isNullOrBlank()) null else JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }
}
