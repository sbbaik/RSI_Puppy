package com.example.rsi_puppy.ui

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "rsi_alert"

    fun notifyRsi(symbol: String, rsi: Double, state: String) {
        ensureChannel()

        val title = "RSI Alert: $symbol"
        val text = "RSI=${"%.2f".format(rsi)} ($state)"

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        manager.notify(symbol.hashCode(), notif)
    }

    private fun ensureChannel() {
        val channel =
            NotificationChannel(channelId, "RSI Alerts", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
    }
}