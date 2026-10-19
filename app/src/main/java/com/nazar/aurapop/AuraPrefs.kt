package com.nazar.aurapop

import android.content.Context
import android.net.Uri

object AuraPrefs {
    private const val FILE = "aurapop_settings"
    private fun prefs(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    const val STYLE_GLASS = "glass"
    const val STYLE_MATERIAL = "material"
    const val STYLE_OLED = "oled"
    const val STYLE_CMF = "cmf"

    fun style(context: Context) = prefs(context).getString("style", STYLE_GLASS) ?: STYLE_GLASS
    fun setStyle(context: Context, value: String) = prefs(context).edit().putString("style", value).apply()
    fun accent(context: Context) = prefs(context).getInt("accent", 0xFF9DF7D6.toInt())
    fun setAccent(context: Context, value: Int) = prefs(context).edit().putInt("accent", value).apply()
    fun systemPalette(context: Context) = prefs(context).getBoolean("system_palette", true)
    fun setSystemPalette(context: Context, value: Boolean) = prefs(context).edit().putBoolean("system_palette", value).apply()
    fun font(context: Context) = prefs(context).getString("font", "system") ?: "system"
    fun setFont(context: Context, value: String) = prefs(context).edit().putString("font", value).apply()
    fun vibration(context: Context) = prefs(context).getInt("vibration", 48).coerceIn(0, 100)
    fun setVibration(context: Context, value: Int) = prefs(context).edit().putInt("vibration", value.coerceIn(0, 100)).apply()
    fun duration(context: Context) = prefs(context).getInt("duration", 5).coerceIn(2, 12)
    fun setDuration(context: Context, value: Int) = prefs(context).edit().putInt("duration", value.coerceIn(2, 12)).apply()
    fun deviceFilter(context: Context) = prefs(context).getString("device_filter", "CMF Buds Pro 2") ?: "CMF Buds Pro 2"
    fun setDeviceFilter(context: Context, value: String) = prefs(context).edit().putString("device_filter", value).apply()
    fun monitoringEnabled(context: Context) = prefs(context).getBoolean("monitoring_enabled", false)
    fun setMonitoringEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean("monitoring_enabled", value).apply()
    fun background(context: Context): Uri? = prefs(context).getString("background_uri", null)?.let { runCatching { Uri.parse(it) }.getOrNull() }
    fun setBackground(context: Context, value: Uri?) = prefs(context).edit().putString("background_uri", value?.toString()).apply()
    fun backgroundIsVideo(context: Context) = prefs(context).getBoolean("background_video", false)
    fun setBackgroundIsVideo(context: Context, value: Boolean) = prefs(context).edit().putBoolean("background_video", value).apply()
    fun showPhoneBattery(context: Context) = prefs(context).getBoolean("show_phone_battery", true)
    fun setShowPhoneBattery(context: Context, value: Boolean) = prefs(context).edit().putBoolean("show_phone_battery", value).apply()
    fun lastVectorLevel(context: Context): Int = prefs(context).getInt("vector_level", -1).takeIf { it in 0..100 } ?: -1
    fun lastVectorName(context: Context): String = prefs(context).getString("vector_name", "") ?: ""
    fun lastVectorAt(context: Context): Long = prefs(context).getLong("vector_at", 0L)
    fun saveVectorLevel(context: Context, level: Int, name: String) {
        if (level !in 0..100) return
        prefs(context).edit().putInt("vector_level", level).putString("vector_name", name).putLong("vector_at", System.currentTimeMillis()).apply()
    }
}
