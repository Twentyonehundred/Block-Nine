#!/usr/bin/env bash
# Build the debug APK and push it straight to the wirelessly-paired phone.
#
# Picks the wireless device by its mDNS name rather than a hard-coded serial, because that
# name changes whenever the phone is re-paired. Prefers the phone over any running emulator.
set -euo pipefail

cd "$(dirname "$0")"

# The Gradle launcher itself needs a JDK 21; org.gradle.java.home only covers the daemon.
export JAVA_HOME=/opt/homebrew/opt/openjdk@21

SDK="$(sed -n 's/^sdk\.dir=//p' local.properties)"
ADB="$SDK/platform-tools/adb"

DEVICE="$("$ADB" devices | awk '/_adb-tls-connect\._tcp[[:space:]]+device$/ {print $1; exit}')"
if [ -z "$DEVICE" ]; then
    echo "No wireless device connected."
    echo "On the phone: Settings > System > Developer options > Wireless debugging,"
    echo "then run:  $ADB pair <ip>:<port> <code>"
    exit 1
fi

./gradlew --quiet assembleDebug
"$ADB" -s "$DEVICE" install -r app/build/outputs/apk/debug/app-debug.apk
echo "Installed to $DEVICE"
