package com.example.dailydrift.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.dailydrift.data.pref.Prefs
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED != intent.action) return

        val prefs = Prefs(context)
        if (!prefs.getBoolean("notifications_enabled", true)) return

        val intervalMinutes = prefs.getInt("hydration_interval", 60)
        val intervalMillis = intervalMinutes * 60_000L

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val broadcast = Intent(context, HydrationReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 0, broadcast,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val startTime = Calendar.getInstance().apply { add(Calendar.MINUTE, intervalMinutes) }.timeInMillis
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startTime, intervalMillis, pending)
    }
}
