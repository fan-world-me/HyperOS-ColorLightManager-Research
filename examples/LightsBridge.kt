package com.example.halolite

import com.example.halolite.lightsapi.ILightsManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object LightsBridge {
    fun getLightsManager(): ILightsManager? {
        if (!Shizuku.pingBinder()) return null
        val raw = SystemServiceHelper.getSystemService(ILightsManager.DESCRIPTOR) ?: return null
        return ILightsManager.Stub.asInterface(ShizukuBinderWrapper(raw))
    }

    fun setCameraRing(colorArgb: Int, onMs: Int) {
        try {
            getLightsManager()?.setCustomLight(
                colorArgb, 0, onMs, 0, 0, "com.android.camera", 12, 0
            )
        } catch (_: Exception) { }
    }

    fun turnOff() = setCameraRing(0, 0)
}
