package com.nazar.aurapop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class VectorBatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val level = intent.getIntExtra("level", -1)
        val name = intent.getStringExtra("name").orEmpty()
        if (level !in 0..100 || name.isBlank()) return
        val filter = AuraPrefs.deviceFilter(context).trim()
        if (filter.isNotEmpty() && !name.contains(filter, true)) return
        AuraPrefs.saveVectorLevel(context, level, name)
        if (!AuraPrefs.monitoringEnabled(context)) return
        try {
            val update = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_UPDATE_BATTERY
                putExtra(OverlayService.EXTRA_DEVICE_NAME, name)
                putExtra(OverlayService.EXTRA_BATTERY_LEVEL, level)
            }
            // This is only an update signal; do not start a new foreground service for it.
            context.startService(update)
        } catch (_: Exception) { }
    }
}
