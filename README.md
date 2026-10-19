# AuraPop — CMF Buds Pro 2 connection pop-up

A native Android Studio project for a customizable floating Bluetooth card, aimed at vivo X200 Pro mini / OriginOS 6. Package: `com.nazar.aurapop`.

## Features in this source build
- Floating overlay created by a foreground service, not by opening the launcher Activity on connection.
- Connection listener for ACL/A2DP Bluetooth broadcasts and a configurable device-name filter (default `CMF Buds Pro 2`).
- Spring/overshoot entrance, brief pop pulse, adjustable vibration, earbud glyph bounce and tap-to-dismiss.
- Liquid-glass inspired, Material You, OLED black and CMF/Nothing-like palettes; accent colours; system accent option; font selection.
- Image or muted looping video background for the card; phone battery from Android's BatteryManager.
- Earbud charge from Android's `BluetoothDevice.getBatteryLevel()` when the OS exposes it. Unknown data is shown as `—%` rather than fabricated.
- Optional legacy Xposed hook in the APK (`assets/xposed_init`) compatible with frameworks that implement the legacy API, including Vector's compatibility layer. It relays system-observed `BluetoothDevice.getBatteryLevel()` values back to the app; it cannot invent separate L/R/case charge values if the firmware does not expose them.
- Separate KernelSU helper module in `kernelSU-module/` for boot-time app-op setup and monitor startup.

## Build APK
1. Open the `AuraPop` folder in Android Studio (JDK 17, Android SDK 36).
2. Let Gradle sync dependencies. The legacy Xposed API is compile-only; it is not bundled into the APK.
3. Use **Build > Build APK(s)**. Debug APK should appear at `app/build/outputs/apk/debug/app-debug.apk`.

This environment did not have Android SDK or Gradle installed, so this source package has not been compiled into an APK here. Treat it as an implementation starter for an Android Studio build, not as a tested production release.

## First run on OriginOS 6
1. Install the APK and open AuraPop.
2. Grant overlay permission and Bluetooth/nearby-device permission. Notification permission is recommended so Android can display the persistent monitor notification.
3. Tap **Включить**, then **Превью** to check the animation.
4. If the monitor gets killed by OriginOS, allow background activity / auto-start for AuraPop in system battery management.
5. Optional root: install `kernelSU-module/AuraPop-Boot-Helper.zip` in KernelSU after first-run permissions are granted.
6. Optional Vector: enable AuraPop in Vector's module list and include relevant scopes (`com.android.systemui`, `com.android.bluetooth`, `com.nazar.aurapop`). Reboot. Hook behaviour depends on Vector's enabled legacy API compatibility and OriginOS implementation.

## Known limitations to verify on your phone
- Earbuds do not always expose battery information through public Android Bluetooth APIs. Many earbuds have separate left/right/case percentages only in their vendor app; this source does not pretend that generic Android APIs always provide them.
- OriginOS may require manual background/auto-start exemptions.
- This source includes the UI, event handling, and module scripts, but needs a real device build/test cycle before calling it bug-free. The initial version is conservative and reversible.
