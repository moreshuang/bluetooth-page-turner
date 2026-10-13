# Initial build verification

Date: 2026-10-13 (Asia/Shanghai). App version: 0.1.0-dev.

## Environment

- macOS arm64; Android Studio bundled JBR 25.0.2.
- Gradle Wrapper 9.1.0, distribution SHA-256 pinned to the official published checksum.
- Android Gradle Plugin 9.0.1 with built-in Kotlin 2.2.10.
- Android SDK Platform 36 revision 2; Build Tools 36.0.0.
- Local SDK/cache directories excluded from Git; SDK licenses reused from the locally accepted SDK license.

## Command and result

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
GRADLE_USER_HOME="$PWD/.build-cache" \
ANDROID_USER_HOME="$PWD/.local-tools/android-user" \
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

Result: **BUILD SUCCESSFUL**.

- Debug APK assembled for minSdk 28 / targetSdk 36.
- 15 JVM unit tests passed, zero failures/errors/skips: keyboard 11, descriptor 2, state 2.
- Android Lint passed with warnings treated as errors. Only `AndroidGradlePluginVersion`, the
  advisory to use newer build-tool versions, is disabled to retain the reviewed pinned toolchain.
- Runtime dependency tree: Kotlin stdlib 2.2.10 → JetBrains annotations 13.0.
- Packaged license assets and merged manifest permissions were inspected.
- No INTERNET, SCAN, ADVERTISE, location, accessibility or foreground-service permission.

The build tools emitted environment warnings about an unwritable global metrics settings file and
sandboxed FSEvents; they did not prevent compilation/tests/lint and do not indicate app telemetry.

## Limits

No Android device was connected (`adb devices` returned an empty list). No app-launch, real
Bluetooth HID registration, host connection, power measurement or Kindle/KOReader test was run.
All hardware compatibility remains pending in DEVICE_TESTING.md. This development APK is debug
signed; no release signing, store publishing or GitHub release has been performed.
