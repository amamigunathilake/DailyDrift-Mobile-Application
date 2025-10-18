package com.example.dailydrift.uii.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.dailydrift.MainActivity
import com.example.dailydrift.R
import com.example.dailydrift.data.repo.HabitRepo
import java.time.LocalDate
import java.time.ZoneId

class HabitWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) updateAppWidget(context, appWidgetManager, id)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            ACTION_REFRESH_WIDGET -> updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.dailydrift.widget.ACTION_REFRESH_WIDGET"

        fun updateAllWidgets(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, HabitWidgetProvider::class.java)
            )
            for (id in ids) updateAppWidget(context, mgr, id)
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val repo = HabitRepo(context)

            val zone = ZoneId.systemDefault()
            val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val todayHabits = repo.getHabits().filter { it.createdAt in start until end }
            val total = todayHabits.size
            val completed = todayHabits.count { it.isCompleted }
            val percent = if (total > 0) (completed * 100) / total else 0

            val views = RemoteViews(context.packageName, R.layout.widget_habit_progress)

            try {
                views.setInt(
                    R.id.widget_root,
                    "setBackgroundColor",
                    context.getColor(R.color.primary_nav_background)
                )
                views.setTextViewText(R.id.widget_app_name, context.getString(R.string.app_name))
                views.setTextColor(R.id.widget_app_name, context.getColor(R.color.text_on_primary))
            } catch (_: Throwable) { /* no-op */ }

            views.setTextViewText(R.id.widget_percentage, "$percent%")
            views.setTextViewText(
                R.id.widget_summary,
                context.getString(R.string.habits_completed, completed, total)
            )
            views.setProgressBar(R.id.widget_progress, 100, percent, false)

            val openIntent = Intent(context, MainActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getActivity(context, 0, openIntent, flags)
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
