package com.onelinecapture

import android.content.Context
import org.json.JSONArray

// 앱 전용 SharedPreferences는 다른 앱이 못 읽는다. 루팅된 기기까지 막을 일은 아니다 (stack.md §8)
class Store(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("capture", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var quickNotification: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION, value).apply()

    // 웹 입력 화면처럼 방금 저장한 것 3개를 보여준다. 서버 목록을 다시 부르지 않으려고 문구만 남긴다
    val recent: List<String>
        get() {
            val array = JSONArray(prefs.getString(KEY_RECENT, "[]"))
            return (0 until array.length()).map { array.getString(it) }
        }

    fun addRecent(line: String) {
        val next = (listOf(line) + recent).take(RECENT_LIMIT)
        prefs.edit().putString(KEY_RECENT, JSONArray(next).toString()).apply()
    }

    fun signOut() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_RECENT).apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_NOTIFICATION = "quick_notification"
        const val KEY_RECENT = "recent"
        const val RECENT_LIMIT = 3
    }
}
