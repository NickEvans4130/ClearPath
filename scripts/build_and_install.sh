#!/usr/bin/env bash
# Build ClearPath debug APK and install to a connected Android device via ADB.
#
# Usage:
#   ./scripts/build_and_install.sh            # debug build + install
#   ./scripts/build_and_install.sh release    # release build + install (needs keystore)
#   ./scripts/build_and_install.sh build      # debug build only, no install
#   ./scripts/build_and_install.sh launch     # build + install + launch app
#
# Prerequisites:
#   - Java 21 at /usr/lib/jvm/java-21-openjdk  (Gradle 8.7 requires <= Java 21)
#   - Android SDK at ~/Android/Sdk
#   - ADB in PATH (or at ~/Android/Sdk/platform-tools/adb)
#   - Device connected via USB with USB debugging enabled
#
# The script automatically falls back to ~/Android/Sdk/platform-tools/adb
# if adb is not on PATH.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# ── Config ────────────────────────────────────────────────────────────────────
JAVA_HOME_21="/usr/lib/jvm/java-21-openjdk"
ANDROID_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
APP_PACKAGE="com.clearpath"
MAIN_ACTIVITY="com.clearpath.MainActivity"

MODE="${1:-debug}"

# ── Resolve ADB ───────────────────────────────────────────────────────────────
if command -v adb &>/dev/null; then
    ADB="adb"
elif [ -x "$ANDROID_SDK/platform-tools/adb" ]; then
    ADB="$ANDROID_SDK/platform-tools/adb"
else
    echo "ERROR: adb not found. Install Android platform-tools or add to PATH."
    exit 1
fi

# ── Validate Java 21 ──────────────────────────────────────────────────────────
if [ ! -x "$JAVA_HOME_21/bin/java" ]; then
    echo "ERROR: Java 21 not found at $JAVA_HOME_21"
    echo "Install with: sudo dnf install java-21-openjdk  (Fedora)"
    echo "           or: sudo apt install openjdk-21-jdk   (Debian/Ubuntu)"
    exit 1
fi

export JAVA_HOME="$JAVA_HOME_21"
export PATH="$JAVA_HOME/bin:$PATH"

# ── Build ─────────────────────────────────────────────────────────────────────
echo "==> Building ClearPath ($MODE)..."
echo "    Java:  $(java -version 2>&1 | head -1)"
echo "    Gradle: $(./gradlew --version --quiet 2>/dev/null | grep '^Gradle' || echo 'unknown')"
echo ""

case "$MODE" in
    release)
        ./gradlew assembleRelease --no-daemon
        APK="app/build/outputs/apk/release/app-release.apk"
        ;;
    build)
        ./gradlew assembleDebug --no-daemon
        echo "==> Build complete: app/build/outputs/apk/debug/app-debug.apk"
        exit 0
        ;;
    launch|debug|*)
        ./gradlew assembleDebug --no-daemon
        APK="app/build/outputs/apk/debug/app-debug.apk"
        ;;
esac

# ── Check device ──────────────────────────────────────────────────────────────
echo ""
echo "==> Checking for connected device..."
DEVICES=$("$ADB" devices | grep -v "^List" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "ERROR: No Android device connected."
    echo "  1. Connect device via USB"
    echo "  2. Enable Developer Options → USB Debugging"
    echo "  3. Accept the authorization prompt on the device"
    exit 1
fi

echo "    Found $DEVICES device(s)"

# ── Install ───────────────────────────────────────────────────────────────────
echo "==> Installing $APK..."
"$ADB" install -r "$APK"
echo ""
echo "==> Installed successfully."

# ── Launch (optional) ─────────────────────────────────────────────────────────
if [ "$MODE" = "launch" ]; then
    echo "==> Launching $APP_PACKAGE..."
    "$ADB" shell am start -n "$APP_PACKAGE/$MAIN_ACTIVITY"
    echo ""
    echo "==> App launched. Streaming logcat (Ctrl-C to stop):"
    "$ADB" logcat --pid=$("$ADB" shell pidof "$APP_PACKAGE" 2>/dev/null | tr -d '\r') \
        -v time 2>/dev/null || "$ADB" logcat -s "ClearPath" -v time
fi

echo ""
echo "Done."
