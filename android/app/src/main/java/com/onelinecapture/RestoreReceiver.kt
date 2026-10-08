package com.onelinecapture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// 재부팅하거나 앱을 업데이트하면 알림이 사라진다. 켜 둔 사람에게는 다시 띄운다
class RestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 시스템이 보낸 두 방송만 받는다. 다른 앱이 아무 인텐트나 보내 알림을 띄우게 두지 않는다
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            QuickNotification.restore(context)
        }
    }
}
