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
open class CaptureWidget : AppWidgetProvider() {

    // 두 크기가 같은 코드를 쓰고 레이아웃만 다르다. 위젯 목록에서 사람이 고른다
    protected open val layout: Int = R.layout.widget_capture

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { manager.updateAppWidget(it, views(context, layout)) }
    }

    companion object {
        // 저장·연결·해제 뒤에 부른다. 위젯은 스스로 새로 고치지 않는다
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            listOf(
                CaptureWidget::class.java to R.layout.widget_capture,
                CaptureWidgetSmall::class.java to R.layout.widget_capture_small,
            ).forEach { (provider, layout) ->
                val ids = manager.getAppWidgetIds(ComponentName(context, provider))
                if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context, layout))
            }
        }

        private fun views(context: Context, layout: Int): RemoteViews {
            val store = Store(context)
            val linked = store.token != null
            val views = RemoteViews(context.packageName, layout)
            val hasRecent = layout == R.layout.widget_capture

            views.setTextViewText(R.id.widget_bar, if (linked) "한 줄 던지기…" else "눌러서 계정 연결")
            if (hasRecent) {
                val recent = store.recent
                views.setTextViewText(R.id.widget_recent, recent.joinToString("\n"))
                views.setViewVisibility(R.id.widget_recent, if (recent.isEmpty()) View.GONE else View.VISIBLE)
            }

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
            if (hasRecent) views.setOnClickPendingIntent(R.id.widget_recent, open)
            return views
        }
    }
}

// 입력칸 한 줄만 있는 작은 위젯 (4×1). 홈 화면 맨 아래 독 위에 두기 좋다
class CaptureWidgetSmall : CaptureWidget() {
    override val layout: Int = R.layout.widget_capture_small
}
