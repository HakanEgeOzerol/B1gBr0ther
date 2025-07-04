package com.b1gbr0ther.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.b1gbr0ther.activities.DashboardActivity
import com.b1gbr0ther.activities.SettingsActivity
import com.b1gbr0ther.R
import com.b1gbr0ther.data.database.entities.Task
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.LocalDate

class TaskNotificationManager(private val context: Context) {
    private val channelId = "task_notifications"
    private val notificationMap = mutableMapOf<Long, Int>()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
    private val notificationPrefs: SharedPreferences = context.getSharedPreferences("TaskNotifications", Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Notifications"
            val descriptionText = "Notifications for task reminders and alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun canSendNotificationToday(taskId: Long, notificationType: String): Boolean {
        val today = LocalDate.now().toString()
        val lastNotificationDate = notificationPrefs.getString("${taskId}_${notificationType}_last_date", "")
        return lastNotificationDate != today
    }

    private fun markNotificationSentToday(taskId: Long, notificationType: String) {
        val today = LocalDate.now().toString()
        notificationPrefs.edit()
            .putString("${taskId}_${notificationType}_last_date", today)
            .apply()
    }

    fun checkAndNotify(task: Task) {
        if (!SettingsActivity.isNotificationsEnabled(sharedPreferences)) {
            return
        }

        val now = LocalDateTime.now()
        val minutesToStart = ChronoUnit.MINUTES.between(now, task.startTime)
        val minutesToEnd = ChronoUnit.MINUTES.between(now, task.endTime)

        when {
            minutesToStart in 10L..20L && now.isBefore(task.startTime) -> {
                if (canSendNotificationToday(task.id, "start_reminder")) {
                    showNotification(
                        task,
                        "Task Starting Soon",
                        "Task '${task.taskName}' is due to begin in 15 minutes",
                        NotificationCompat.PRIORITY_HIGH
                    )
                    markNotificationSentToday(task.id, "start_reminder")
                }
            }
            minutesToEnd in 10L..20L && !task.isCompleted && now.isAfter(task.startTime) && now.isBefore(task.endTime) -> {
                if (canSendNotificationToday(task.id, "end_reminder")) {
                    showNotification(
                        task,
                        "Task Ending Soon",
                        "Task '${task.taskName}' is due to end in 15 minutes",
                        NotificationCompat.PRIORITY_HIGH
                    )
                    markNotificationSentToday(task.id, "end_reminder")
                }
            }
            !task.isCompleted && now.isAfter(task.endTime) && minutesToEnd <= -1L -> {
                if (canSendNotificationToday(task.id, "overdue")) {
                    showNotification(
                        task,
                        "Task Overdue",
                        "Task '${task.taskName}' is overdue!",
                        NotificationCompat.PRIORITY_HIGH
                    )
                    markNotificationSentToday(task.id, "overdue")
                }
            }
        }
    }

    private fun showNotification(task: Task, title: String, content: String, priority: Int) {
        if (!SettingsActivity.isNotificationsEnabled(sharedPreferences)) {
            return
        }

        try {
            val intent = Intent(context, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                task.id.toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(priority)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))

            with(NotificationManagerCompat.from(context)) {
                notify(task.id.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun resetNotificationCount(taskId: Long) {
        notificationMap.remove(taskId)
        // Also clear the daily notification tracking for this task
        val today = LocalDate.now().toString()
        notificationPrefs.edit()
            .remove("${taskId}_start_reminder_last_date")
            .remove("${taskId}_end_reminder_last_date")
            .remove("${taskId}_overdue_last_date")
            .apply()
    }
} 