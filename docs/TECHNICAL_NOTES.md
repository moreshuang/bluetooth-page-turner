# Technical feasibility and implementation boundaries

Checked against primary sources on 2026-10-13.

## Platform API

[`BluetoothHidDevice`](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice)
was introduced in API 28 (Android 9). The app requests
[`BluetoothProfile.HID_DEVICE`](https://developer.android.com/reference/android/bluetooth/BluetoothProfile#HID_DEVICE)
through [`BluetoothAdapter.getProfileProxy`](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#getProfileProxy(android.content.Context,%20android.bluetooth.BluetoothProfile.ServiceListener,%20int)).
There is no public definitive preflight flag guaranteeing the manufacturer's HID Device implementation.
An absent adapter / rejected profile request is shown as unsupported; a service timeout is reported
as inconclusive, with a retry, rather than a definitive hardware diagnosis.

Only one HID app can register at a time. Registration disables the phone's HID Host service until
unregistration, so concurrently using a Bluetooth keyboard attached to the phone may be affected.
Android automatically unregisters a non-foreground app. The MVP explicitly cleans up in onStop;
returning to the Activity acquires a new proxy/registers again and requires manual host connection.
This also applies to Bluetooth settings and screen-off. No foreground service is present.

`registerApp`, `connect`, `disconnect`, and `sendReport` booleans acknowledge commands, not final
state or remote input delivery. This app uses callbacks for state, and a 10-second profile/registration
timeout and 20-second connection timeout. A timeout invalidates the session to ignore late callbacks.

## Permissions

[`Bluetooth permissions`](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
distinguishes existing-device communication (CONNECT), discovery (SCAN), and local discoverability
(ADVERTISE). This app lists bonded devices and uses HID commands; only CONNECT is requested on API 31+.

API 28–30 uses legacy BLUETOOTH and BLUETOOTH_ADMIN, both capped at maxSdkVersion 30.
Although this app does not discover devices or change adapter settings, the legacy HID service
enforces ADMIN on registerApp, unregisterApp, connect, disconnect and report operations.
SCAN, ADVERTISE and location are omitted. The system Settings application owns pairing.
A future in-app discoverability action would need a new permission review.

The [`Android 10 HID service`](https://android.googlesource.com/platform/packages/apps/Bluetooth/+/refs/tags/android-10.0.0_r47/src/com/android/bluetooth/hid/HidDeviceService.java)
is an additional primary reference for legacy permission enforcement; no implementation is copied.

Android 13+ uses explicit receiver flags for the protected Bluetooth state broadcast. Android 15/16
edge-to-edge system bars are handled using window insets. No Android 17-specific APIs are used; the
app's target is 36 and newer OS behavior remains a hardware test item.

## Keyboard wire format

Descriptor items are independently assembled using
[USB-IF HID 1.11](https://www.usb.org/document-library/device-class-definition-hid-111) and
[USB-IF HID Usage Tables](https://www.usb.org/document-library/hid-usage-tables-17).
These are specifications, not copied implementation source or vendored document text.

- Keyboard/Keypad usage page: 0x07.
- Right Arrow 0x4F; Left Arrow 0x50; Page Up 0x4B; Page Down 0x4E.
- Report protocol input ID 1: modifier byte, reserved byte, six key usage bytes.
- Boot protocol input ID 0: the same eight-byte payload.
- ID is passed separately to Android's `sendReport`; it is not prepended to payload data.
- One LED output byte is described and accepted; it does not control page turning.
- GET_REPORT responds with the current input state or stored LED byte; invalid requests receive
  a handshake error. Host output reports and protocol changes are handled.

The 40 ms hold is a starting value to verify, not a guaranteed host debounce interval. No repeated
keydown reports are sent. Failed key-up attempts are retried twice at 30 ms intervals; persistent
failure unregisters/disconnects the session. Link loss cannot guarantee delivery of a final key-up;
host behavior after disconnection must be checked manually.

## Build versions

minSdk 28 preserves the official API floor; compileSdk/targetSdk 36 covers Android 16 without
introducing APIs newer than 28 into the HID implementation. AGP 9.0.1 provides built-in Kotlin 2.2.10.
Gradle 9.1.0 supports the installed JBR 25; JDK 17 or 21 is recommended for contributors.

Sources: [AGP 9.0 compatibility](https://developer.android.com/build/releases/agp-9-0-0-release-notes),
[Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html).

## Host boundary

This is Bluetooth Classic HID, not BLE HOGP, audio control, accessibility input injection or a
Kindle-specific protocol. A Kindle/Kobo with Bluetooth audio support may still lack keyboard-host
support. Kindle-side HID event forwarding and KOReader key handling must be validated separately.
No third-party Kindle integration was installed, copied or checked for device-specific suitability.
