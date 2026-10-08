package com.onelinecapture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build

// 알림창에 늘 떠 있는 "한 줄 입력" 칸. 잠금화면에서도 보이고, 답장 칸에 치면 바로 저장된다 (spec.md §9)
object QuickNotification {

    const val KEY_TEXT = "text"
    private const val CHANNEL = "quick_capture"
    private const val ID = 1

    fun show(context: Context, line: String? = null) {
        val manager = context.getSystemService(NotificationManager::class.java)
        // 소리·진동이 없어야 한다. 늘 떠 있는 알림이 울리면 바로 꺼 버린다
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "빠른 입력", NotificationManager.IMPORTANCE_LOW).apply {
                description = "알림창과 잠금화면에서 바로 한 줄을 저장합니다"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            },
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT).setLabel("점심 9000원").build()
        // 답장 결과를 받으려면 MUTABLE이어야 한다. 시스템이 입력값을 이 인텐트에 채워 넣는다
        val reply = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReplyReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val action = Notification.Action.Builder(null, "한 줄 입력", reply)
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .apply {
                // 쓰기만 하는 동작이라 잠금을 풀지 않아도 저장되게 한다. 이게 잠금화면 입력의 핵심이다
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAuthenticationRequired(false)
            }
            .build()
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("한 줄 캡처")
            .setContentText(line ?: "여기서 바로 한 줄을 저장합니다")
            .setContentIntent(open)
            .addAction(action)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
        manager.notify(ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(ID)
    }

    // 껐거나 연결이 끊겼으면 띄우지 않는다. 부팅·앱 업데이트 뒤에 다시 띄울 때도 이걸 거친다
    fun restore(context: Context) {
        val store = Store(context)
        if (store.quickNotification && store.token != null) show(context) else cancel(context)
    }
}
