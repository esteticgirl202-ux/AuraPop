# Device verification checklist — vivo X200 Pro mini / OriginOS 6

This is a build checklist, not a claim that these tests have already passed.

- [ ] Install the debug APK and launch it once.
- [ ] Grant `Display over other apps`, `Bluetooth / Nearby devices`, and notification access when asked.
- [ ] Start monitoring from AuraPop. Confirm the persistent low-importance notification appears.
- [ ] Tap Preview: card should enter with a spring/energy pulse and dismiss on tapping the card; tapping the earbud glyph should bounce it.
- [ ] Connect CMF Buds Pro 2 and confirm only one card appears despite duplicate ACL/A2DP broadcasts.
- [ ] Compare the shown charge with Android Bluetooth settings / CMF app. If Android does not expose it, verify that the card displays `—%` rather than a false number.
- [ ] Test each style, system palette, font, photo and a muted looping video background.
- [ ] Test with AuraPop in the background, screen locked, after reboot, and after several minutes of idle time.
- [ ] If Vector is enabled, verify its legacy hook appears in logs and the optional battery relay is scoped only to relevant packages.
- [ ] Disable/uninstall the KernelSU module and confirm manual mode still works.
