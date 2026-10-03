#!/usr/bin/env bash
# Fast compile-only check. No APK output — just verifies Kotlin compiles.
# Used by the PostToolUse hook to catch errors before CI.
set -euo pipefail
cd "$(dirname "$0")/../.."
./gradlew :app:compileDebugKotlin "$@"
echo "Compile OK"
