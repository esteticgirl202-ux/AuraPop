package com.nazar.aurapop

import android.bluetooth.BluetoothDevice
import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.AndroidAppHelper

/** Optional Vector bridge: relays Bluetooth battery values observed by hooked system processes.
 * It never fabricates missing battery percentages and does not alter Bluetooth behavior. */
class VectorBatteryHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val watched = setOf("com.android.systemui", "com.android.bluetooth", "com.nazar.aurapop", "android")
        if (lpparam.packageName !in watched) return
        try {
            XposedHelpers.findAndHookMethod(
                BluetoothDevice::class.java,
                "getBatteryLevel",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val level = (param.result as? Int) ?: return
                        if (level !in 0..100) return
                        val device = param.thisObject as? BluetoothDevice ?: return
                        val name = try { device.name.orEmpty() } catch (_: Throwable) { "" }
                        if (!name.contains("CMF", true) && !name.contains("Buds", true)) return
                        val app = AndroidAppHelper.currentApplication() ?: return
                        val out = Intent("com.nazar.aurapop.VECTOR_BATTERY")
                            .setPackage("com.nazar.aurapop")
                            .putExtra("level", level)
                            .putExtra("name", name)
                        try { app.sendBroadcast(out) } catch (t: Throwable) { XposedBridge.log("AuraPop: battery relay failed: $t") }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("AuraPop: Vector battery hook unavailable in ${lpparam.packageName}: $t")
        }
    }
}
