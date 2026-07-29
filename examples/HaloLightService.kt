package com.example.halolite

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.*

class HaloLightService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        job?.cancel()
        job = scope.launch {
            val stepMs = 35L               // как часто меняем оттенок — быстрее переливание
            val holdMs = 300                // сколько "держим" цвет на аппаратном уровне —
                                             // с запасом больше stepMs, чтобы не успевало
                                             // погаснуть само между обновлениями
            val durationMs = 5000L
            val steps = (durationMs / stepMs).toInt()
            val hueStep = 360f / steps
            var hue = 0f
            repeat(steps) {
                val color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                LightsBridge.setCameraRing(color, holdMs)
                hue = (hue + hueStep) % 360f
                delay(stepMs)
            }
            LightsBridge.turnOff()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val channelId = "halolite_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(channelId) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(channelId, "HaloLite", NotificationManager.IMPORTANCE_MIN)
                )
            }
        }
        return NotificationCompatBuilder(this, channelId)
    }
}

@Suppress("FunctionName")
private fun NotificationCompatBuilder(ctx: Context, channelId: String): Notification {
    val builder = Notification.Builder(ctx, channelId)
    builder.setContentTitle("HaloLite")
    builder.setContentText("Радуга...")
    builder.setSmallIcon(android.R.drawable.ic_dialog_info)
    builder.setOngoing(true)
    return builder.build()
}
