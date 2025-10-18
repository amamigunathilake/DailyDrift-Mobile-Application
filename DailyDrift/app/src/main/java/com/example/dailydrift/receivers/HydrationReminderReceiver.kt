package com.example.dailydrift.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.dailydrift.MainActivity
import com.example.dailydrift.R
import com.example.dailydrift.data.pref.Prefs
import com.example.dailydrift.data.repo.HydrationRepo

class HydrationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = Prefs(context)

        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        val hydrationEnabled = prefs.getBoolean("hydration_enabled", false)
        if (!notificationsEnabled || !hydrationEnabled) return

        val goalMl = prefs.getInt("hydration_goal_ml", 2000)
        val intervalMin = prefs.getInt("hydration_interval", 60)
        val total = HydrationRepo(context).totalToday()
        val percent = if (goalMl > 0) (total * 100) / goalMl else 0

        createChannelIfNeeded(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_DEST, MainActivity.DEST_SETTINGS)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, 0, openIntent, flags)

        val title = context.getString(R.string.hydration_reminders)
        val content = "Total today: ${total} ml  •  Goal: ${goalMl} ml"
        val subText = "Every ${intervalMin} min"

        val bubble = makeWaterBubbleBitmap(percent.coerceIn(0, 100), 512)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(title)
            .setContentText(content)
            .setSubText(subText)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bubble)
                    .bigLargeIcon(null as Bitmap?)
            )
            .build()

        NotificationManagerCompat.from(context).notify(NOTI_ID, notification)
    }

    private fun makeWaterBubbleBitmap(percent: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val cx = size / 2f
        val cy = size / 2f
        val r = size * 0.42f

        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, size.toFloat(),
                Color.parseColor("#6EA8FF"),
                Color.parseColor("#3F7BFD"),
                Shader.TileMode.CLAMP
            )
        }
        c.drawCircle(cx, cy, r, bubblePaint)

        val save = c.save()
        val path = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
        c.clipPath(path)

        val levelH = cy + r - (2 * r) * (percent / 100f)
        val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#88FFFFFF")
        }
        c.drawRect(cx - r, levelH, cx + r, cy + r, waterPaint)
        c.restoreToCount(save)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = size * 0.18f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        }
        val textY = cy - (textPaint.descent() + textPaint.ascent()) / 2
        c.drawText("$percent%", cx, textY, textPaint)

        return bmp
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to drink water at your chosen interval."
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "hydration_reminders"
        private const val NOTI_ID = 1001
    }
}
