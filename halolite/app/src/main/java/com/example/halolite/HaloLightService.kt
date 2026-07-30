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
            val stepMs = 25L                 // быстрее смена цветов
            val halfDurationMs = 2500L        // половина на "туда", половина на "обратно"
            val stepsPerHalf = (halfDurationMs / stepMs).toInt()
            val hueStep = 360f / stepsPerHalf

            // Проход 1: красный -> весь спектр -> обратно к красному (0 -> 360)
            var hue = 0f
            repeat(stepsPerHalf) {
                LightsBridge.setCameraRing(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)), stepMs.toInt())
                hue = (hue + hueStep) % 360f
                delay(stepMs)
            }
            // Проход 2: обратно (360 -> 0), тоже заканчивается на красном
            hue = 360f
            repeat(stepsPerHalf) {
                hue = (hue - hueStep + 360f) % 360f
                LightsBridge.setCameraRing(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)), stepMs.toInt())
                delay(stepMs)
            }

            // гарантированно фиксируем чистый красный в самом конце
            LightsBridge.setCameraRing(Color.HSVToColor(floatArrayOf(0f, 1f, 1f)), 100)
            delay(150)
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
