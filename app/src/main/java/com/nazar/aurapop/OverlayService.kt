package com.nazar.aurapop

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.Vibrator
import android.app.VibratorManager
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.core.app.NotificationCompat
import kotlin.math.max

class OverlayService : Service() {
    companion object {
        const val ACTION_START = "com.nazar.aurapop.action.START"
        const val ACTION_PREVIEW = "com.nazar.aurapop.action.PREVIEW"
        const val ACTION_CONNECTED = "com.nazar.aurapop.action.CONNECTED"
        const val ACTION_UPDATE_BATTERY = "com.nazar.aurapop.action.UPDATE_BATTERY"
        const val ACTION_STOP = "com.nazar.aurapop.action.STOP"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_BATTERY_LEVEL = "battery_level"
        private const val CHANNEL_ID = "aurapop_monitor"
        private const val NOTIFICATION_ID = 4821
    }

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayRoot: FrameLayout? = null
    private var overlayCard: FrameLayout? = null
    private var deviceName = "CMF Buds Pro 2"
    private var batteryLevel = -1
    private var phoneBatteryLevel = -1
    private var shownAt = 0L
    private var dismissRunnable: Runnable? = null
    private var videoView: VideoView? = null
    private var foreground = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        phoneBatteryLevel = readPhoneBattery()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                AuraPrefs.setMonitoringEnabled(this, false)
                removeOverlay(animate = false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PREVIEW -> {
                ensureForeground()
                showOverlay("CMF Buds Pro 2", 82, force = true)
            }
            ACTION_CONNECTED -> {
                ensureForeground()
                val name = intent.getStringExtra(EXTRA_DEVICE_NAME) ?: "Bluetooth headphones"
                val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                showOverlay(name, level, force = false)
            }
            ACTION_UPDATE_BATTERY -> {
                if (AuraPrefs.monitoringEnabled(this)) ensureForeground()
                val name = intent.getStringExtra(EXTRA_DEVICE_NAME).orEmpty()
                val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                if (overlayRoot != null && (name.isBlank() || name.equals(deviceName, true))) {
                    if (level in 0..100) {
                        batteryLevel = level
                        refreshBatteryLabels()
                    }
                }
            }
            ACTION_START -> {
                AuraPrefs.setMonitoringEnabled(this, true)
                ensureForeground()
            }
            else -> {
                if (AuraPrefs.monitoringEnabled(this)) ensureForeground() else stopSelf()
            }
        }
        return START_STICKY
    }

    private fun ensureForeground() {
        if (foreground) return
        createNotificationChannel()
        val launchIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("AuraPop работает")
            .setContentText("Жду подключения CMF Buds Pro 2")
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        foreground = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "AuraPop — подключение наушников", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Фоновый монитор Bluetooth для всплывающей плашки"
                setShowBadge(false)
            })
        }
    }

    private fun showOverlay(name: String, level: Int, force: Boolean) {
        if (!android.provider.Settings.canDrawOverlays(this)) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (!force && now - shownAt < 1800L) return
        shownAt = now
        deviceName = name
        batteryLevel = level.takeIf { it in 0..100 } ?: -1
        if (batteryLevel == -1 && AuraPrefs.lastVectorName(this).equals(name, true) && System.currentTimeMillis() - AuraPrefs.lastVectorAt(this) < 10 * 60 * 1000) {
            batteryLevel = AuraPrefs.lastVectorLevel(this)
        }
        phoneBatteryLevel = readPhoneBattery()
        removeOverlay(animate = false)

        val root = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(dp(13), 0, dp(13), 0)
        }
        val card = FrameLayout(this).apply {
            background = cardDrawable()
            clipToOutline = true
            elevation = dp(10).toFloat()
        }

        addBackground(card)
        val scrim = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xB6000000.toInt(), 0x70000000, 0xB8000000.toInt()))
            isClickable = false
        }
        card.addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val content = buildCardContent()
        card.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        card.setOnClickListener { removeOverlay(animate = true) }
        root.addView(card, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(148)))
        root.setOnClickListener { removeOverlay(animate = true) }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(160),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = statusBarHeight() + dp(9)
            windowAnimations = 0
        }
        try {
            windowManager?.addView(root, params)
            overlayRoot = root
            overlayCard = card
            root.alpha = 0f
            root.translationY = -dp(48).toFloat()
            root.scaleX = 0.94f
            root.scaleY = 0.94f
            root.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(440).setInterpolator(OvershootInterpolator(0.8f)).withEndAction {
                pulseAndVibrate()
            }.start()
            scheduleDismiss()
        } catch (_: Exception) {
            overlayRoot = null
            overlayCard = null
            releaseVideo()
        }
    }

    private fun buildCardContent(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(17), dp(13), dp(17), dp(11))
            isClickable = false
        }
        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val title = TextView(this).apply {
            text = deviceName
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = fontTypeface(true)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val connected = TextView(this).apply {
            text = "●  CONNECTED"
            textSize = 9.5f
            letterSpacing = 0.04f
            setTextColor(resolveAccent())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        topRow.addView(title)
        topRow.addView(connected)
        column.addView(topRow)

        val middle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, dp(2))
        }
        val glyph = EarbudsGlyph(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(69), dp(57))
            scaleX = 0.68f
            scaleY = 0.68f
            rotation = -9f
            alpha = 0f
            postDelayed({
                animate().alpha(1f).scaleX(1f).scaleY(1f).rotation(0f)
                    .setDuration(390).setInterpolator(OvershootInterpolator(2.6f)).start()
            }, 65L)
            setOnClickListener {
                animate().scaleX(0.78f).scaleY(0.78f).setDuration(75).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(260).setInterpolator(OvershootInterpolator(3.4f)).start()
                }.start()
                smallHaptic()
            }
        }
        middle.addView(glyph)

        val batteryColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val batLabel = TextView(this).apply {
            tag = "aurapop_battery_label"
            text = "НАУШНИКИ"
            textSize = 9f
            letterSpacing = 0.12f
            setTextColor(0xD8FFFFFF.toInt())
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        val batValue = TextView(this).apply {
            tag = "aurapop_battery_value"
            text = batteryText()
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = fontTypeface(true)
        }
        batteryColumn.addView(batLabel)
        batteryColumn.addView(batValue)
        middle.addView(batteryColumn)

        if (AuraPrefs.showPhoneBattery(this)) {
            val phoneCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(7), 0, 0, 0)
            }
            phoneCol.addView(TextView(this).apply {
                text = "ТЕЛЕФОН"
                textSize = 8.5f
                letterSpacing = 0.08f
                setTextColor(0xD8FFFFFF.toInt())
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            phoneCol.addView(TextView(this).apply {
                tag = "aurapop_phone_value"
                text = if (phoneBatteryLevel in 0..100) "$phoneBatteryLevel%" else "—"
                textSize = 18f
                setTextColor(resolveAccent())
                typeface = fontTypeface(true)
            })
            middle.addView(phoneCol)
        }
        column.addView(middle)
        column.addView(TextView(this).apply {
            text = "Коснись плашки, чтобы закрыть"
            textSize = 10f
            setTextColor(0xC8FFFFFF.toInt())
            typeface = fontTypeface(false)
        })
        return column
    }

    private fun addBackground(card: FrameLayout) {
        val uri = AuraPrefs.background(this) ?: return
        try {
            if (AuraPrefs.backgroundIsVideo(this)) {
                val video = VideoView(this).apply {
                    isClickable = false
                    isFocusable = false
                    setVideoURI(uri)
                    setOnPreparedListener { player: MediaPlayer ->
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                }
                videoView = video
                card.addView(video, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            } else {
                val image = ImageView(this).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    isClickable = false
                    isFocusable = false
                }
                card.addView(image, 0, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                // Decode a downsampled image off the main thread to avoid a large-photo OOM/jank.
                Thread {
                    val bitmap = try {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
                            var sample = 1
                            while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 700) sample *= 2
                            val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
                            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                        }
                    } catch (_: Exception) { null }
                    if (bitmap != null) image.post { if (image.parent != null) image.setImageBitmap(bitmap) }
                }.start()
            }
        } catch (_: Exception) { }
    }

    private fun refreshBatteryLabels() {
        val root = overlayRoot ?: return
        root.findViewWithTag<TextView>("aurapop_battery_value")?.text = batteryText()
        root.findViewWithTag<TextView>("aurapop_phone_value")?.text = if (phoneBatteryLevel in 0..100) "$phoneBatteryLevel%" else "—"
    }

    private fun batteryText() = if (batteryLevel in 0..100) "$batteryLevel%" else "—%"

    private fun scheduleDismiss() {
        dismissRunnable?.let(handler::removeCallbacks)
        val duration = AuraPrefs.duration(this) * 1000L
        dismissRunnable = Runnable { removeOverlay(animate = true) }
        handler.postDelayed(dismissRunnable!!, duration)
    }

    private fun removeOverlay(animate: Boolean) {
        dismissRunnable?.let(handler::removeCallbacks)
        dismissRunnable = null
        val root = overlayRoot ?: return
        overlayRoot = null
        overlayCard = null
        if (animate) {
            root.animate().alpha(0f).translationY(-dp(20).toFloat()).scaleX(0.96f).scaleY(0.96f).setDuration(170).withEndAction {
                try { windowManager?.removeView(root) } catch (_: Exception) { }
                releaseVideo()
                if (!AuraPrefs.monitoringEnabled(this)) {
                    handler.postDelayed({
                        if (overlayRoot == null && !AuraPrefs.monitoringEnabled(this)) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            foreground = false
                            stopSelf()
                        }
                    }, 40L)
                }
            }.start()
        } else {
            try { root.animate().cancel(); windowManager?.removeView(root) } catch (_: Exception) { }
            releaseVideo()
        }
    }

    private fun releaseVideo() {
        try { videoView?.stopPlayback() } catch (_: Exception) { }
        videoView = null
    }

    private fun pulseAndVibrate() {
        val card = overlayCard ?: return
        val value = AuraPrefs.vibration(this)
        if (value > 0) {
            try {
                val vibrator = vibratorInstance()
                val amplitude = (value * 255 / 100).coerceIn(1, 255)
                if (Build.VERSION.SDK_INT >= 26 && vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 18, 26, 16), intArrayOf(0, amplitude, 0, max(1, amplitude / 2)), -1))
                } else if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE))
                } else vibrator.vibrate(24)
            } catch (_: Exception) { }
        }
        card.animate().scaleX(1.025f).scaleY(1.025f).setDuration(85).withEndAction {
            card.animate().scaleX(1f).scaleY(1f).setDuration(180).setInterpolator(OvershootInterpolator(2.4f)).start()
        }.start()
    }

    private fun smallHaptic() {
        try {
            val value = AuraPrefs.vibration(this)
            if (value <= 0) return
            val vibrator = vibratorInstance()
            if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(12, (value * 255 / 100).coerceAtLeast(1))) else vibrator.vibrate(12)
        } catch (_: Exception) { }
    }

    private fun cardDrawable(): GradientDrawable {
        val style = AuraPrefs.style(this)
        val accent = resolveAccent()
        val colors = when (style) {
            AuraPrefs.STYLE_MATERIAL -> intArrayOf(blend(accent, 0xFF182024.toInt(), 0.78f), 0xF51A2024.toInt())
            AuraPrefs.STYLE_OLED -> intArrayOf(0xF70A0A0C.toInt(), 0xF7121214.toInt())
            AuraPrefs.STYLE_CMF -> intArrayOf(0xF63B4146.toInt(), 0xF11A1D20.toInt())
            else -> intArrayOf(blend(accent, 0xFF3D4551.toInt(), 0.35f), 0xC91A202A.toInt())
        }
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(29).toFloat()
            setStroke(dp(1), when (style) {
                AuraPrefs.STYLE_OLED -> 0xFF333333.toInt()
                AuraPrefs.STYLE_GLASS -> 0x67FFFFFF
                else -> 0x3FFFFFFF
            })
        }
    }

    private fun resolveAccent(): Int {
        if (!AuraPrefs.systemPalette(this)) return AuraPrefs.accent(this)
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                val id = resources.getIdentifier("system_accent1_300", "color", "android")
                if (id != 0) return resources.getColor(id, theme)
            } catch (_: Exception) { }
        }
        return try {
            val array = obtainStyledAttributes(intArrayOf(android.R.attr.colorAccent))
            val color = array.getColor(0, AuraPrefs.accent(this))
            array.recycle()
            color
        } catch (_: Exception) { AuraPrefs.accent(this) }
    }

    private fun fontTypeface(bold: Boolean): Typeface = when (AuraPrefs.font(this)) {
        "mono" -> Typeface.create("monospace", if (bold) Typeface.BOLD else Typeface.NORMAL)
        "serif" -> Typeface.create("serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        "rounded" -> Typeface.create("sans-serif-rounded", if (bold) Typeface.BOLD else Typeface.NORMAL)
        else -> Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun readPhoneBattery(): Int = try {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 } ?: -1
    } catch (_: Exception) { -1 }

    @Suppress("DEPRECATION")
    private fun vibratorInstance(): Vibrator = if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun blend(a: Int, b: Int, factorA: Float): Int {
        val f = factorA.coerceIn(0f, 1f)
        fun component(shift: Int) = (((a shr shift) and 0xFF) * f + ((b shr shift) and 0xFF) * (1f - f)).toInt().coerceIn(0, 255)
        return Color.argb(component(24), component(16), component(8), component(0))
    }

    override fun onDestroy() {
        removeOverlay(animate = false)
        dismissRunnable?.let(handler::removeCallbacks)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private class EarbudsGlyph(context: Context) : View(context) {
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF3F5F5.toInt(); style = Paint.Style.FILL }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9DF7D6.toInt(); style = Paint.Style.FILL }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xAAFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = resources.displayMetrics.density * 1.4f }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = resources.displayMetrics.density
        val scale = minOf(width / (72f * s), height / (58f * s))
        val save = canvas.save()
        canvas.translate((width - 72f * s * scale) / 2f, (height - 58f * s * scale) / 2f)
        canvas.scale(scale, scale)

        val left = Path().apply {
            moveTo(13f, 17f); cubicTo(13f, 7f, 20f, 2f, 27f, 5f); cubicTo(35f, 8f, 35f, 18f, 29f, 23f)
            lineTo(26f, 27f); lineTo(26f, 46f); cubicTo(26f, 51f, 19f, 52f, 18f, 46f); lineTo(18f, 25f); cubicTo(15f, 23f, 13f, 21f, 13f, 17f); close()
        }
        canvas.drawPath(left, bodyPaint)
        canvas.drawPath(left, outline)
        val right = Path().apply {
            moveTo(42f, 17f); cubicTo(42f, 7f, 49f, 2f, 56f, 5f); cubicTo(64f, 8f, 64f, 18f, 58f, 23f)
            lineTo(55f, 27f); lineTo(55f, 46f); cubicTo(55f, 51f, 48f, 52f, 47f, 46f); lineTo(47f, 25f); cubicTo(44f, 23f, 42f, 21f, 42f, 17f); close()
        }
        canvas.drawPath(right, bodyPaint)
        canvas.drawPath(right, outline)
        canvas.drawCircle(23f, 15f, 3.2f, accentPaint)
        canvas.drawCircle(52f, 15f, 3.2f, accentPaint)
        canvas.restoreToCount(save)
    }
}
