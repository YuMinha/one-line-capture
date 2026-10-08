package com.onelinecapture

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

// 홈 화면(지원하는 기기는 잠금화면) 위젯. 위젯 안에서는 글자를 칠 수 없어서(안드로이드 제약)
// 입력칸을 누르면 키보드가 달린 작은 입력창이 뜬다 (spec.md §9)
class CaptureWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context)) }
    }

    companion object {
        // 저장·연결·해제 뒤에 부른다. 위젯은 스스로 새로 고치지 않는다
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CaptureWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context))
        }

        private fun views(context: Context): RemoteViews {
            val store = Store(context)
            val linked = store.token != null
            val views = RemoteViews(context.packageName, R.layout.widget_capture)

            views.setTextViewText(R.id.widget_bar, if (linked) "한 줄 던지기…" else "눌러서 계정 연결")
            val recent = store.recent
            views.setTextViewText(R.id.widget_recent, recent.joinToString("\n"))
            views.setViewVisibility(R.id.widget_recent, if (recent.isEmpty()) View.GONE else View.VISIBLE)

            val target = if (linked) QuickCaptureActivity::class.java else MainActivity::class.java
            val input = PendingIntent.getActivity(
                context, 1,
                Intent(context, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val open = PendingIntent.getActivity(
                context, 2, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_bar, input)
            views.setOnClickPendingIntent(R.id.widget_recent, open)
            return views
        }
    }
}
