# Bluetooth Page Turner

Turn an Android phone into a Bluetooth HID remote for page navigation.

Bluetooth Page Turner is an MIT-licensed Android application for readers who want a
simple, local page-turning remote. It uses the standard Bluetooth Classic HID keyboard
profile to send navigation keys to a paired host.

**Project status:** early development (`0.1.0-dev`). The current implementation provides
on-screen Previous and Next controls. Physical volume-button control is planned after
hardware validation of the initial HID connection and page-navigation workflow.

## Features

- Standard HID keyboard registration through Android's `BluetoothHidDevice` API.
- Paired-device selection with manual connection and disconnection.
- Previous and Next controls mapped to Left Arrow and Right Arrow.
- Timed key release, connection status, and actionable error messages.
- Foreground operation with session cleanup when leaving the app.
- JVM unit tests for keyboard reports, descriptor structure, and key-pulse timing.

## Requirements

- Android 9 (API 28) or later, with Bluetooth HID Device Profile support.
- A paired host that supports Bluetooth Classic HID keyboards.
- A reading application that handles arrow-key navigation.

Keep the app in the foreground while using the current version. Profile availability
and input handling depend on the phone, host firmware, and reading application.

## How it works

```text
Android phone → Bluetooth HID keyboard → Paired host → Page navigation
```

The phone sends a short key-down/key-up sequence for each button press. The host's
reading application interprets the navigation key. The app uses a single Android
module, platform UI controls, and a small Kotlin runtime dependency set.

Implementation details and primary references are documented in
[Technical notes](docs/TECHNICAL_NOTES.md).

## Build

Install JDK 17 or 21, Android SDK Platform 36, and Android SDK Build Tools 36.0.0.
The project includes Gradle Wrapper 9.1.0 and uses Android Gradle Plugin 9.0.1.

Open the project in Android Studio, or configure the SDK path in `local.properties`:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

Build the debug APK and run the project checks:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows, use `gradlew.bat`. The first build downloads the required build dependencies.
The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

The initial build passed compilation, 15 JVM unit tests, and Android Lint.
See the [build verification record](docs/BUILD_VERIFICATION.md) for the tested environment.

## Usage

1. Install the debug APK and enable Bluetooth on the phone and host.
2. Complete pairing through the system Bluetooth settings.
3. Open the app. On Android 12 or later, select **Refresh / Register HID** and grant
   the Nearby devices permission.
4. Wait for **HID ready**, select the paired host, and choose **Connect**.
5. Once the status is **Connected**, open a document on the host and use **Previous**
   and **Next** to navigate.
6. Choose **Disconnect** to end the session.

Pairing is handled by the system. Returning from Bluetooth settings starts a fresh HID
session; reconnect from the app before testing page navigation. Some hosts require the
keyboard's HID service information during initial pairing. Record the pairing sequence
when reporting compatibility results.

For setup, troubleshooting, and acceptance checks, see the
[device testing guide](docs/DEVICE_TESTING.md).

## Android support and permissions

The project uses `minSdk 28` and `compileSdk` / `targetSdk 36`.

| Android version | API level | Required permissions |
| --- | --- | --- |
| 9–11 | 28–30 | `BLUETOOTH` and `BLUETOOTH_ADMIN`, granted at installation |
| 12–12L | 31–32 | `BLUETOOTH_CONNECT`, granted through Nearby devices |
| 13 | 33 | `BLUETOOTH_CONNECT` |
| 14 | 34 | `BLUETOOTH_CONNECT` |
| 15 | 35 | `BLUETOOTH_CONNECT` |
| 16 | 36 | `BLUETOOTH_CONNECT` |
| Later versions | 37+ | Hardware validation pending; current implementation uses the CONNECT API contract |

Legacy permissions are limited to API 30. The legacy HID service requires
`BLUETOOTH_ADMIN` for registration, connection, and report operations. Runtime permissions
on Android 12 and later are limited to communication with paired devices.

## Compatibility

The compatibility scope includes e-readers, tablets, and desktop computers with a
Bluetooth Classic HID host implementation. Reader key bindings and host input forwarding
are part of device validation.

| Host category | Requirements | Validation status |
| --- | --- | --- |
| E-readers | Keyboard HID input and reader key bindings | Pending |
| Tablets | Bluetooth keyboard support and reader navigation | Pending |
| Desktop computers | Bluetooth Classic HID host and application key handling | Pending |

Hardware testing is the next development milestone. Please include the environment and
results described in the [device testing guide](docs/DEVICE_TESTING.md) when contributing
a compatibility report.

## Privacy

The app operates locally over Bluetooth, with all application functionality available
offline. Paired-device names and addresses are held in memory for host selection and
connection. Runtime communication is limited to Bluetooth, and data retention is limited
to the active app session.

Diagnostic logging records HID lifecycle events and key types. Review platform exception
output before sharing logs, as system-generated messages may include device information.
Android cloud backup and device-transfer rules exclude application data. The current
implementation uses callback-driven Bluetooth state handling; power consumption remains
a hardware validation item.

## Roadmap

| Milestone | Focus |
| --- | --- |
| v0.1.0 | HID registration, paired-host connection, on-screen controls, and hardware validation |
| v0.2.0 | Foreground volume-button control and configurable key mappings |
| v0.3.0 | Foreground service evaluation, screen-off operation, and optional reconnection |
| v0.4.0 | Long presses, double presses, and profiles |

Further platform integrations will be evaluated as use cases and compatibility results
become available. See [CHANGELOG.md](CHANGELOG.md) for the development history.

## Contributing

Bug reports, device compatibility reports, documentation improvements, and focused code
contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the development
workflow and contribution requirements.

## License

Project code is released under the [MIT License](LICENSE).
Copyright © 2026 Xiong Chao.

Dependency licenses and attribution requirements are recorded in
[DEPENDENCIES.md](DEPENDENCIES.md). Upstream license texts are retained in
[third_party/licenses](third_party/licenses).
