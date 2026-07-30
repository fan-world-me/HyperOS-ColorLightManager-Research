package com.example.halolite

import android.util.Log
import com.example.halolite.lightsapi.ILightsManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object LightsBridge {
    private const val TAG = "HaloLite"

    fun getLightsManager(): ILightsManager? {
        if (!Shizuku.pingBinder()) return null
        val raw = SystemServiceHelper.getSystemService(ILightsManager.DESCRIPTOR) ?: return null
        return ILightsManager.Stub.asInterface(ShizukuBinderWrapper(raw))
    }

    fun setCameraRing(colorArgb: Int, onMs: Int) {
        try {
            // Эксперимент: setColorCommon(styleType=3) не имеет таймера
            // автовыключения в коде сервиса, в отличие от setCustomLight —
            // должно не мигать при частых обновлениях.
            getLightsManager()?.setColorCommon(colorArgb, "com.android.camera", 3, 0)
        } catch (e: Exception) {
            Log.e(TAG, "setCameraRing failed", e)
        }
    }

    fun turnOff() = setCameraRing(0, 0)
}
