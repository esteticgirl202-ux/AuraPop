package com.nazar.aurapop

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            val scheme = if (Build.VERSION.SDK_INT >= 31) {
                runCatching { dynamicDarkColorScheme(this@MainActivity) }.getOrElse { darkColorScheme(primary = Color(0xFF9DF7D6)) }
            } else darkColorScheme(primary = Color(0xFF9DF7D6), background = Color(0xFF101214), surface = Color(0xFF1C2024))
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AuraSettings()
                }
            }
        }
    }
}

private data class StyleChoice(val key: String, val label: String, val subtitle: String)
private val styles = listOf(
    StyleChoice(AuraPrefs.STYLE_GLASS, "Liquid Glass", "Полупрозрачное стекло"),
    StyleChoice(AuraPrefs.STYLE_MATERIAL, "Material You", "Акцент системы"),
    StyleChoice(AuraPrefs.STYLE_OLED, "OLED Black", "Чёрный без лишнего"),
    StyleChoice(AuraPrefs.STYLE_CMF, "CMF / Nothing", "Контрастный техно-стиль")
)
private val colors = listOf(
    0xFF9DF7D6.toInt(), 0xFFB7C5FF.toInt(), 0xFFFFC6A5.toInt(),
    0xFFFFAFC8.toInt(), 0xFFE7EF9D.toInt(), 0xFFB7F0FF.toInt(), 0xFFFFFFFF.toInt()
)

@Composable
private fun AuraSettings() {
    val context = LocalContext.current
    var selectedStyle by remember { mutableStateOf(AuraPrefs.style(context)) }
    var selectedAccent by remember { mutableIntStateOf(AuraPrefs.accent(context)) }
    var systemPalette by remember { mutableStateOf(AuraPrefs.systemPalette(context)) }
    var selectedFont by remember { mutableStateOf(AuraPrefs.font(context)) }
    var vibration by remember { mutableIntStateOf(AuraPrefs.vibration(context)) }
    var duration by remember { mutableIntStateOf(AuraPrefs.duration(context)) }
    var deviceFilter by remember { mutableStateOf(AuraPrefs.deviceFilter(context)) }
    var backgroundUri by remember { mutableStateOf(AuraPrefs.background(context)) }
    var backgroundVideo by remember { mutableStateOf(AuraPrefs.backgroundIsVideo(context)) }
    var showPhoneBattery by remember { mutableStateOf(AuraPrefs.showPhoneBattery(context)) }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var serviceActive by remember { mutableStateOf(AuraPrefs.monitoringEnabled(context)) }
    var permissionHint by remember { mutableStateOf("Сначала разреши показ поверх других приложений и доступ Bluetooth.") }

    fun startMonitoring() {
        val intent = Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_START)
        try {
            if (Build.VERSION.SDK_INT >= 26) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
            serviceActive = true
            permissionHint = "Мониторинг включён. Подключи CMF Buds Pro 2 для проверки."
        } catch (e: Exception) {
            permissionHint = "Не удалось запустить мониторинг: открой AuraPop и проверь права OriginOS."
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // Notification permission is optional for monitoring; FGS will still use its service notification.
        startMonitoring()
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else startMonitoring()
        } else permissionHint = "Без доступа Bluetooth нельзя прочитать имя и заряд подключённых наушников."
    }
    val overlaySettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasOverlay = Settings.canDrawOverlays(context)
        permissionHint = if (hasOverlay) "Разрешение получено. Теперь включи мониторинг." else "Разрешение не выдано — плашке некуда рисоваться."
    }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
            val type = context.contentResolver.getType(uri).orEmpty()
            backgroundVideo = type.startsWith("video/")
            backgroundUri = uri
            AuraPrefs.setBackground(context, uri)
            AuraPrefs.setBackgroundIsVideo(context, backgroundVideo)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(52.dp).background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF9DF7D6), Color(0xFF8DB7FF))),
                    shape = RoundedCornerShape(18.dp)
                ), contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.BluetoothConnected, contentDescription = null, tint = Color(0xFF101719), modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("AuraPop", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.7).sp)
                Text("Живые плашки для CMF Buds Pro 2", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Мониторинг", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(if (serviceActive) "ВКЛ" else "ВЫКЛ", color = if (serviceActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(permissionHint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = {
                            hasOverlay = Settings.canDrawOverlays(context)
                            if (!hasOverlay) {
                                overlaySettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                            } else if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else startMonitoring()
                        }, modifier = Modifier.weight(1f)
                    ) { Text(if (serviceActive) "Мониторинг включён" else "Включить") }
                    OutlinedButton(onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            overlaySettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        } else if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            val i = Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_PREVIEW)
                            try { if (Build.VERSION.SDK_INT >= 26) ContextCompat.startForegroundService(context, i) else context.startService(i) }
                            catch (_: Exception) { permissionHint = "OriginOS заблокировал запуск. Проверь права и автозапуск." }
                        }
                    }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(3.dp))
                        Text("Превью")
                    }
                }
                OutlinedButton(
                    onClick = {
                        context.startService(Intent(context, OverlayService::class.java).setAction(OverlayService.ACTION_STOP))
                        serviceActive = false
                        permissionHint = "Мониторинг остановлен."
                    }, modifier = Modifier.fillMaxWidth()
                ) { Text("Остановить мониторинг") }
            }
        }

        SectionCard(title = "Внешний вид", icon = { Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) {
            Text("Стиль плашки", fontWeight = FontWeight.SemiBold)
            styles.forEach { choice ->
                val selected = selectedStyle == choice.key
                Surface(
                    onClick = { selectedStyle = choice.key; AuraPrefs.setStyle(context, choice.key) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(choice.label, fontWeight = FontWeight.SemiBold, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                            Text(choice.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Выбрано", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Палитра", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                colors.forEach { colorValue ->
                    Surface(
                        onClick = { selectedAccent = colorValue; AuraPrefs.setAccent(context, colorValue); systemPalette = false; AuraPrefs.setSystemPalette(context, false) },
                        modifier = Modifier.size(39.dp), shape = CircleShape,
                        color = Color(colorValue), border = if (selectedAccent == colorValue && !systemPalette) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null
                    ) {
                        if (selectedAccent == colorValue && !systemPalette) Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF151719), modifier = Modifier.size(17.dp)) }
                    }
                }
            }
            SettingSwitch(
                title = "Цвета Material You",
                subtitle = "Брать системный акцент Android, если он доступен",
                checked = systemPalette,
                onCheckedChange = { systemPalette = it; AuraPrefs.setSystemPalette(context, it) }
            )
            Text("Шрифт", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("system" to "Системный", "rounded" to "Мягкий", "mono" to "Моно", "serif" to "Serif").forEach { (key, label) ->
                    FilterChip(selected = selectedFont == key, onClick = { selectedFont = key; AuraPrefs.setFont(context, key) }, label = {
                        Text(label, fontFamily = when (key) { "mono" -> FontFamily.Monospace; "serif" -> FontFamily.Serif; else -> FontFamily.SansSerif })
                    })
                }
            }
        }

        SectionCard(title = "Фон и энергия", icon = { Icon(Icons.Rounded.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (backgroundUri == null) "Фон по умолчанию" else if (backgroundVideo) "Видео выбрано" else "Фото выбрано", fontWeight = FontWeight.SemiBold)
                    Text("Фото или короткое видео внутри плашки", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                Button(onClick = { mediaPicker.launch(arrayOf("image/*", "video/*")) }) { Text("Выбрать") }
            }
            if (backgroundUri != null) OutlinedButton(onClick = {
                backgroundUri = null
                backgroundVideo = false
                AuraPrefs.setBackground(context, null)
                AuraPrefs.setBackgroundIsVideo(context, false)
            }, modifier = Modifier.fillMaxWidth()) { Text("Убрать фон") }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingSwitch(
                title = "Показывать заряд телефона",
                subtitle = "Рядом с зарядом наушников",
                checked = showPhoneBattery,
                onCheckedChange = { showPhoneBattery = it; AuraPrefs.setShowPhoneBattery(context, it) }
            )
            Text("Сила вибрации · $vibration%", fontWeight = FontWeight.SemiBold)
            Slider(value = vibration.toFloat(), onValueChange = { vibration = it.toInt(); AuraPrefs.setVibration(context, vibration) }, valueRange = 0f..100f, steps = 9)
            Text("Время показа · $duration сек", fontWeight = FontWeight.SemiBold)
            Slider(value = duration.toFloat(), onValueChange = { duration = it.toInt().coerceIn(2, 12); AuraPrefs.setDuration(context, duration) }, valueRange = 2f..12f, steps = 9)
        }

        SectionCard(title = "Подключение", icon = { Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }) {
            Text("Имя Bluetooth-устройства", fontWeight = FontWeight.SemiBold)
            TextField(
                value = deviceFilter,
                onValueChange = { deviceFilter = it; AuraPrefs.setDeviceFilter(context, it) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(15.dp),
                placeholder = { Text("CMF Buds Pro 2") }
            )
            Text("Плашка появляется при совпадении имени. Оставь поле пустым, чтобы реагировать на любые Bluetooth-устройства.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("KernelSU + Vector", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Обычная плашка работает через Android overlay. В архиве есть KernelSU-модуль автозапуска и Vector-хук, который может передавать системе AuraPop доступные ей данные батареи. Если OriginOS не сообщает заряд каждого наушника отдельно, модуль не сможет достать отсутствующие данные магией.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionCard(title: String, icon: @Composable () -> Unit, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                icon()
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
