package com.nazar.aurapop

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BluetoothConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AuraPrefs.monitoringEnabled(context)) return
        val action = intent.action ?: return
        if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) return

        if (action == BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) {
            val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
            if (state != BluetoothProfile.STATE_CONNECTED) return
        }

        val device: BluetoothDevice? = try {
            if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        } catch (_: SecurityException) { null }

        val nameFromIntent = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
        val name = try { device?.name } catch (_: SecurityException) { null } ?: nameFromIntent ?: "Bluetooth headphones"
        val filter = AuraPrefs.deviceFilter(context).trim()
        if (filter.isNotEmpty() && !name.contains(filter, ignoreCase = true)) return

        var level = -1
        if (Build.VERSION.SDK_INT >= 33 && device != null) {
            level = try { device.batteryLevel } catch (_: Throwable) { -1 }
        }
        if (level !in 0..100 && AuraPrefs.lastVectorName(context).equals(name, true) &&
            System.currentTimeMillis() - AuraPrefs.lastVectorAt(context) < 10 * 60 * 1000) {
            level = AuraPrefs.lastVectorLevel(context)
        }
        val service = Intent(context, OverlayService::class.java).apply {
            action = OverlayService.ACTION_CONNECTED
            putExtra(OverlayService.EXTRA_DEVICE_NAME, name)
            putExtra(OverlayService.EXTRA_BATTERY_LEVEL, level)
        }
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service) else context.startService(service)
        } catch (_: Exception) {
            // OriginOS may block background starts until the user enables autostart/battery exemptions.
        }
    }
}
