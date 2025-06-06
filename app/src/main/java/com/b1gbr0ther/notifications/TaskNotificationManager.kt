package com.b1gbr0ther.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.b1gbr0ther.DashboardActivity
import com.b1gbr0ther.R
import com.b1gbr0ther.data.database.entities.Task
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TaskNotificationManager(private val context: Context) {
    private val channelId = "task_notifications"
    private val notificationMap = mutableMapOf<Long, Int>()

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

    fun checkAndNotify(task: Task) {
        if (notificationMap.getOrDefault(task.id, 0) >= 3) return

        val now = LocalDateTime.now()
        val minutesToStart = ChronoUnit.MINUTES.between(now, task.startTime)
        val minutesToEnd = ChronoUnit.MINUTES.between(now, task.endTime)

        when {
            minutesToStart in 14..15 && now.isBefore(task.startTime) -> {
                showNotification(
                    task,
                    "Task Starting Soon",
                    "Task '${task.taskName}' will start in 15 minutes",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
            minutesToEnd in 14..15 && !task.isCompleted && now.isAfter(task.startTime) && now.isBefore(task.endTime) -> {
                showNotification(
                    task,
                    "Task Ending Soon",
                    "Task '${task.taskName}' will end in 15 minutes",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
            !task.isCompleted && now.isAfter(task.endTime) && minutesToEnd <= -1 -> {
                showNotification(
                    task,
                    "Task Overdue",
                    "Task '${task.taskName}' is overdue!",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
        }
    }

    private fun showNotification(task: Task, title: String, content: String, priority: Int) {
        try {
            val currentCount = notificationMap.getOrDefault(task.id, 0)
            if (currentCount >= 3) return
            notificationMap[task.id] = currentCount + 1

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
    }
} 