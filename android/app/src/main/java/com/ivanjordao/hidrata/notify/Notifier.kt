package com.ivanjordao.hidrata.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ivanjordao.hidrata.MainActivity
import com.ivanjordao.hidrata.R
import com.ivanjordao.hidrata.data.HidrataRepository

/** Monta e exibe a notificação, com os mesmos botões do alerta do PWA. */
object Notifier {
    private const val CHANNEL_ID = "lembretes"
    private const val NOTIF_ID = 1
    const val BANNER_CUP = 300

    /** No Android 8+ toda notificação pertence a um "canal", que o usuário pode ajustar nas configurações. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Lembretes de água", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisa quando você está prestes a ficar ou já ficou abaixo da meta"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** No Android 13+ é preciso pedir permissão para notificar. */
    fun hasPermission(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    @android.annotation.SuppressLint("MissingPermission") // a permissão é conferida em hasPermission()
    fun show(context: Context, title: String, body: String): Boolean {
        if (!hasPermission(context)) return false
        createChannel(context)

        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Bebi $BANNER_CUP ml", actionIntent(context, ActionReceiver.ACTION_DRINK, 1))
            .addAction(0, "Lembrar em ${HidrataRepository.SNOOZE_MIN} min", actionIntent(context, ActionReceiver.ACTION_SNOOZE, 2))
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(NOTIF_ID)

    private fun actionIntent(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, ActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

/** Recebe os toques nos botões da notificação, sem precisar abrir o app. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = HidrataRepository.get(context)
        when (intent.action) {
            ACTION_DRINK -> repo.addWater(Notifier.BANNER_CUP)
            ACTION_SNOOZE -> repo.snooze()
        }
        Notifier.cancel(context)
    }

    companion object {
        const val ACTION_DRINK = "com.ivanjordao.hidrata.DRINK"
        const val ACTION_SNOOZE = "com.ivanjordao.hidrata.SNOOZE"
    }
}
