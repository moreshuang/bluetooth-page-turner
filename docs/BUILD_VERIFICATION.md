# Build verification

Initial verification date: 2026-10-13 (Asia/Shanghai). App version: `0.1.0-dev`.
Registration fix verification date: 2026-10-14 (Asia/Shanghai). Debug assembly, all 15 JVM
unit tests, and Android Lint passed again with the same toolchain.
Page-control UI verification date: 2026-10-14 (Asia/Shanghai). The same build checks
passed after the layout update. The debug APK was installed on Xiaomi 13 and the
connected layout was captured and inspected at 1080 × 2400 pixels. ADB taps on both
enlarged controls logged the expected Left and Right Arrow actions.

## Environment

| Component | Tested version |
| --- | --- |
| Host | macOS arm64 |
| JDK | Android Studio JBR 25.0.2 |
| Gradle Wrapper | 9.1.0, official distribution SHA-256 pinned |
| Android Gradle Plugin | 9.0.1 |
| Kotlin | 2.2.10, built into AGP |
| Android SDK Platform | API 36, revision 2 |
| Android SDK Build Tools | 36.0.0 |

SDK and dependency caches were isolated in local directories excluded from Git.
The build used the locally accepted Android SDK license.

## Reproduction

Configure `JAVA_HOME` for a supported JDK and set `sdk.dir` in `local.properties`.
From the project root, run:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

The initial macOS environment selected the Android Studio JDK and local caches explicitly:

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
GRADLE_USER_HOME="$PWD/.build-cache" \
ANDROID_USER_HOME="$PWD/.local-tools/android-user" \
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

## Results

Result: **BUILD SUCCESSFUL**.

| Check | Result |
| --- | --- |
| Debug APK | Built for minSdk 28 / targetSdk 36 |
| JVM unit tests | 15 passed: keyboard 11, descriptor 2, state 2 |
| Android Lint | Passed with warnings treated as errors |
| Runtime dependency tree | Kotlin stdlib 2.2.10 → JetBrains annotations 13.0 |
| Packaged notices | Project and runtime license assets inspected |
| Manifest permissions | Bluetooth permissions matched the documented API ranges |
| APK signature | Debug signature verified |

The advisory `AndroidGradlePluginVersion` Lint check is disabled so toolchain upgrades
follow a deliberate compatibility and license review. Other project Lint checks remain enabled.

Environment-specific warnings concerned the global metrics settings file and FSEvents.
Compilation, tests, and Lint completed successfully.

## Hardware validation

The initial verification covered the build artifact and JVM behavior. The registration
fix was subsequently installed and checked on a physical phone:

| Item | Observation on 2026-10-14 |
| --- | --- |
| Phone | Xiaomi 13, model 2211133G |
| System | Android 13, MIUI build V14.0.31.0.TMCCNXM |
| Permissions | Nearby devices permission granted |
| Registration | `registerApp` returned `false`; `onAppStatusChanged` confirmed registration |
| UI state | `HID ready · No device selected` |
| Refresh | Registration recovered and UI returned to ready |
| Home and reopen | Application registered again and UI returned to ready |

On the same date, the maintainer completed pairing, host connection, and Next/Previous
page navigation with a Kindle Paperwhite 6 and KOReader. The
[physical device validation report](HARDWARE_VALIDATION.md) records the exact host
configuration, connection evidence, and screenshots. Longer-duration input, key-release,
and power checks remain pending.

The current artifact is a debug-signed development APK. Release signing and distribution
are subsequent release-management tasks.
