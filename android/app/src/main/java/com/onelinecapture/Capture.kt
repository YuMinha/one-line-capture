package com.onelinecapture

import android.content.Context
import android.os.Handler
import android.os.Looper

// 앱 화면·빠른 입력창·알림창 답장이 모두 이 한 곳으로 저장한다. 결과 문구와 토큰 처리가 어긋나지 않게
object Capture {

    sealed interface Result {
        data class Saved(val message: String) : Result
        data class Failed(val message: String, val signedOut: Boolean) : Result
    }

    private val main = Handler(Looper.getMainLooper())

    // 네트워크는 메인 스레드에서 못 부른다. 저장 한 번에 코루틴 라이브러리까지 들일 일은 아니라 스레드 하나를 쓴다
    fun save(context: Context, text: String, done: (Result) -> Unit) {
        val store = Store(context)
        Thread {
            val result = saveBlocking(store, text)
            main.post { done(result) }
        }.start()
    }

    fun saveBlocking(store: Store, text: String): Result {
        val token = store.token ?: return Result.Failed("먼저 계정을 연결해 주세요", signedOut = true)
        return try {
            val message = CaptureText.saved(CaptureApi.create(token, text))
            store.addRecent(message)
            Result.Saved(message)
        } catch (e: ApiException) {
            if (e.tokenRejected) {
                // 웹에서 이 기기를 로그아웃했거나 토큰이 사라졌다. 다시 연결하게 한다
                store.signOut()
            }
            Result.Failed(e.message ?: "저장하지 못했습니다", e.tokenRejected)
        }
    }
}
