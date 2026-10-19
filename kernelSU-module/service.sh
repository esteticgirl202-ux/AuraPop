#!/system/bin/sh
# AuraPop's KernelSU service hook. Runs as root after Android boot.
MODDIR=${0%/*}
APP="com.nazar.aurapop"

# Wait until package manager and user 0 are available.
count=0
while [ "$(getprop sys.boot_completed)" != "1" ] && [ "$count" -lt 90 ]; do
  sleep 2
  count=$((count + 1))
done

# Do nothing if AuraPop has not been installed yet.
if ! cmd package list packages "$APP" 2>/dev/null | grep -q "$APP"; then
  exit 0
fi

# Allow the overlay AppOp using root. The user still must grant Bluetooth permission
# in AuraPop once, since runtime permissions cannot safely be silently forged here.
cmd appops set --user 0 "$APP" SYSTEM_ALERT_WINDOW allow >/dev/null 2>&1

# Start the monitor only after the app has been opened once and permissions granted.
am start-foreground-service --user 0 -n "$APP/.OverlayService" -a "$APP.action.START" >/dev/null 2>&1
exit 0
