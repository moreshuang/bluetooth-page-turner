# Technical notes

Primary references reviewed on 2026-10-13 and 2026-10-14.

## Android HID integration

[`BluetoothHidDevice`](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice)
is available from API 28 (Android 9). The app requests
[`BluetoothProfile.HID_DEVICE`](https://developer.android.com/reference/android/bluetooth/BluetoothProfile#HID_DEVICE)
through [`BluetoothAdapter.getProfileProxy`](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#getProfileProxy(android.content.Context,%20android.bluetooth.BluetoothProfile.ServiceListener,%20int)).

Profile availability is determined during service acquisition and registration. An absent
adapter or rejected profile request produces an unsupported status. A service timeout
produces a retryable error, leaving profile availability subject to further validation.

Android allows one registered HID application at a time. Registration temporarily disables
the phone's HID Host service until unregistration, which can affect keyboards attached to
the phone. Android also unregisters applications that leave the foreground. The current
implementation manages the HID session through the Activity lifecycle and performs cleanup
in `onStop`. Returning to the Activity acquires a new proxy and registers again; the user
then connects the selected host manually.

Registration and connection callbacks determine application state; hardware tests establish
input delivery and page-navigation behavior. Registration retains its callback and proxy
until Android reports the result or the request times out, including when `registerApp`
returns `false`. On the tested Xiaomi 13 / Android 13 build, the call returned `false`
and a successful registration callback followed. This follows the API's documented
separation between the method return value and registration status.
The profile/registration timeout is 10 seconds, and the connection timeout is 20 seconds.
A timeout invalidates the session so late callbacks are handled as obsolete.

## Permissions

The [official permission guide](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
distinguishes paired-device communication (`CONNECT`), discovery (`SCAN`), and local
discoverability (`ADVERTISE`). This implementation lists paired devices and uses HID commands.
Android 12 and later require `BLUETOOTH_CONNECT` at runtime.

API 28–30 requires `BLUETOOTH` and `BLUETOOTH_ADMIN`, both limited to `maxSdkVersion 30`.
The legacy HID service enforces ADMIN for registration, connection, disconnection, and report
operations. The [Android 10 HID service](https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/tags/android-10.0.0_r47/src/com/android/bluetooth/hid/HidDeviceService.java)
provides the primary reference for this behavior.

Pairing takes place outside the app through Android settings or the host's pairing
interface. The tested Kindle used its local host API; see the
[physical device validation report](HARDWARE_VALIDATION.md). Changes to discovery or
pairing behavior should include a review of the corresponding permissions and user
interaction.

The app uses explicit receiver flags for the protected Bluetooth state broadcast on Android
13 and later. Window insets handle system bars, including enforced edge-to-edge layouts on
Android 15 and 16. Validation on later Android releases is tracked through device testing.

## Keyboard reports

Descriptor items are independently assembled from
[USB-IF HID 1.11](https://www.usb.org/document-library/device-class-definition-hid-111) and
[HID Usage Tables](https://www.usb.org/document-library/hid-usage-tables-17).

| Field | Value |
| --- | --- |
| Usage page | Keyboard/Keypad, `0x07` |
| Right Arrow / Left Arrow | `0x4F` / `0x50` |
| Page Up / Page Down | `0x4B` / `0x4E` |
| Report protocol input ID | `1` |
| Boot protocol input ID | `0` |
| Input payload | 8 bytes: modifiers, reserved byte, six key usages |
| LED output payload | 1 byte |

The report ID is passed separately to Android's `sendReport` API. `GET_REPORT` returns the
current input state or stored LED byte. Invalid requests receive a handshake error.
Output reports and protocol changes are handled through the HID callbacks.

Each input pulse holds a key for 40 ms before sending an all-zero release report. This
initial timing is subject to hardware validation. During a pulse, page controls are briefly
disabled to serialize input. A failed release receives two retries at 30 ms intervals;
persistent failure triggers HID session teardown. Intentional disconnection attempts a
final release. Host behavior during link loss is included in lifecycle acceptance testing.

## Code organization

| Component | Responsibility |
| --- | --- |
| `HidManager` | Profile ownership, registration, connection, state, and lifecycle cleanup |
| `HidDescriptor` | Keyboard HID descriptor and report constants |
| `HidKeyboard` | Report generation and key-pulse scheduling |
| `HidKey` | Standard keyboard usage IDs |
| `MainActivity` / `MainScreen` | Foreground lifecycle, permissions, and platform UI controls |

Protocol encoding and timing are covered by JVM tests. Platform behavior is validated
through the [device testing guide](DEVICE_TESTING.md).

## Build configuration

The API floor is `minSdk 28`; `compileSdk` and `targetSdk` are 36. AGP 9.0.1 provides
built-in Kotlin 2.2.10. Gradle Wrapper 9.1.0 supports the JBR 25 used for the initial build;
JDK 17 or 21 is recommended for contributors.

References: [AGP compatibility](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
and [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html).

## Host integration

The transport is Bluetooth Classic HID keyboard input. Compatibility depends on the host's
keyboard profile, event forwarding, reading-application focus, and key bindings. E-reader
firmware may require host-side keyboard input integration. These responsibilities are
validated independently from Android registration and report generation.
