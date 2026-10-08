package com.onelinecapture

import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReplyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(QuickNotification.KEY_TEXT)?.toString()?.trim()
        if (text.isNullOrEmpty()) {
            // 같은 알림을 다시 띄워야 입력칸의 로딩 표시가 멈춘다
            QuickNotification.show(context)
            return
        }

        // 리시버는 10초 안에 끝나야 한다. 네트워크를 기다리는 동안 죽지 않게 goAsync로 붙잡는다
        val pending = goAsync()
        Thread {
            try {
                when (val result = Capture.saveBlocking(Store(context), text)) {
                    is Capture.Result.Saved -> QuickNotification.show(context, result.message)
                    // 오프라인 쓰기 큐는 없다 (spec.md §6). 원문을 알림에 남겨 다시 칠 수 있게 한다
                    is Capture.Result.Failed ->
                        if (result.signedOut) QuickNotification.cancel(context)
                        else QuickNotification.show(context, "저장 실패: ${result.message} — \"$text\"")
                }
            } finally {
                pending.finish()
            }
        }.start()
    }
}
