package app.trainer.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.trainer.MainActivity
import app.trainer.R
import app.trainer.TrainerApp
import app.trainer.core.ReminderTime
import kotlinx.coroutines.flow.first
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Daily local reminder. Each run schedules the next one under the *other* of two unique names,
 * so a worker never replaces its own (still running) work.
 */
object ReminderScheduler {
    private const val SLOT_KEY = "slot"
    private fun name(slot: Int) = "daily_reminder_$slot"

    fun apply(context: Context, settings: ReminderSettings) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(name(0))
        wm.cancelUniqueWork(name(1))
        if (settings.enabled) schedule(context, settings, slot = 0)
    }

    fun schedule(context: Context, settings: ReminderSettings, slot: Int) {
        val delay = ReminderTime.delayUntilNextMillis(ZonedDateTime.now(), settings.hour, settings.minute)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putInt(SLOT_KEY, slot).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name(slot), ExistingWorkPolicy.REPLACE, request)
    }

    internal fun slotOf(data: Data) = data.getInt(SLOT_KEY, 0)
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = (applicationContext as TrainerApp).settings.reminder.first()
        if (!settings.enabled) return Result.success()
        notifyUser(applicationContext)
        ReminderScheduler.schedule(applicationContext, settings, slot = 1 - ReminderScheduler.slotOf(inputData))
        return Result.success()
    }

    private fun notifyUser(context: Context) {
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, notification)
    }

    companion object { const val CHANNEL_ID = "daily_reminder" }
}
