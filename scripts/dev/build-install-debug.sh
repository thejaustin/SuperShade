#!/usr/bin/env bash
# Build debug APK and install directly to device via ADB loopback.
# Usage: bash scripts/dev/build-install-debug.sh
set -euo pipefail
cd "$(dirname "$0")/../.."

ADB_SERIAL="${ADB_SERIAL:-127.0.0.1:5555}"

echo "▸ Connecting ADB ($ADB_SERIAL)..."
adb connect "$ADB_SERIAL" 2>/dev/null || true

echo "▸ Building :app:assembleDebug..."
./gradlew :app:assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "❌ APK not found at $APK"
  exit 1
fi

echo "▸ Installing ($APK)..."
adb -s "$ADB_SERIAL" install -r -d "$APK"
echo "✓ Installed SuperShade debug APK"
