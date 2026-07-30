package com.example.halolite

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private fun shizukuReady(): Boolean =
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, grant ->
        if (grant == PackageManager.PERMISSION_GRANTED) {
            startService(Intent(this, HaloLightService::class.java))
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (shizukuReady()) {
            setTheme(android.R.style.Theme_NoDisplay)
            super.onCreate(savedInstanceState)
            startService(Intent(this, HaloLightService::class.java))
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permListener)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 200, 48, 48)
        }
        layout.addView(TextView(this).apply {
            text = if (Shizuku.pingBinder())
                "Shizuku найден. Нажми, чтобы выдать разрешение."
            else
                "Shizuku не запущен. Запусти Shizuku, потом вернись сюда."
        })
        layout.addView(Button(this).apply {
            text = "Подключить"
            setOnClickListener {
                if (Shizuku.pingBinder()) {
                    Shizuku.requestPermission(1)
                } else {
                    finish()
                }
            }
        })
        setContentView(layout)
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }
}
