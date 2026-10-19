# AuraPop Boot Helper (KernelSU)

This small module grants AuraPop's overlay AppOp at boot and starts its persistent Bluetooth monitor. It does not patch framework files and can be removed from KernelSU Manager.

## Install order
1. Install and open `AuraPop` once.
2. In AuraPop, grant **Display over other apps**, **Nearby devices / Bluetooth**, and notification permission when requested. Start the monitor once to verify it works.
3. Install `AuraPop-Boot-Helper.zip` in KernelSU Manager and reboot.
4. In OriginOS battery management, allow AuraPop background activity / autostart if those controls exist on your build.

The module is deliberately a boot helper only: all visual settings remain in the APK, and uninstalling the module leaves no changes to system APKs.
