# Connection development plan

Planning date: **2026-11-16 (Asia/Shanghai)**.

**Status:** planned work for the next connection milestone. Implementation and
acceptance results will be recorded as each stage is completed.

## Goal and feasibility

Enable a reader to open the phone app, start pairing, select the phone by name on
the host, confirm pairing, and turn pages. Subsequent sessions should offer a
one-tap connection to the previously selected host. The user completes setup
through the phone and host interfaces, with device addresses handled internally.

The recorded Xiaomi 13 and Kindle configuration has already completed HID
registration, pairing, connection, and page navigation in both directions. This
establishes a working input path after connection. The next milestone focuses on
discovery, first-time pairing, session ownership, and reconnection.

The assessment is **feasible with moderate implementation and device-validation
work**. Android provides public discoverability and HID APIs. The tested host's
scan filtering is a compatibility dependency to verify during the prototype.
Delivering the complete experience on that host may require a focused host-side
change alongside the Android work.

## Evidence and open questions

The baseline is the [LineageOS validation report](LINEAGEOS_VALIDATION.md), published
in documentation commit [`393e5e1`](https://github.com/moreshuang/bluetooth-page-turner/commit/393e5e180c7d3f35bdce222cda608ada4bf884fd).
The tested APK uses app source commit `28c8ecc`.

| Finding | Evidence and development implication |
| --- | --- |
| Input delivery works after connection | The maintainer confirmed Next and Previous page navigation on Xiaomi 13 / Android 16 / LineageOS 23.2 with Kindle Paperwhite 6 and KOReader. Preserve the existing keyboard descriptor and key-release behavior. |
| The app currently lists bonded devices | `HidManager.refreshDevices()` reads `bondedDevices`. Add an explicit first-time pairing flow. |
| Phone discoverability has an official API | Use `ACTION_REQUEST_DISCOVERABLE` with system confirmation and a limited pairing window. Add `BLUETOOTH_ADVERTISE` for Android 12 and later. |
| The tested host filters Classic scan results by device category | Version `3.17.5-ba48659` includes only the `Peripheral` major class. A phone-class result would be excluded. |
| The cause of the observed empty scan list needs a controlled test | The phone's configured Class of Device property was `90,2,12`. Capture the actual inquiry result while HID registration and discoverability are active to establish what the host receives. |
| Current session ownership follows the Activity | `MainActivity.onStop()` calls `hid.stop()`. Moving to Settings ends the current registration and connection. Evaluate service ownership for the pairing and reading session. |
| This test phone needed a system profile setting | Enabling `bluetooth.profile.hid.device.enabled` at runtime restored the HID service. The setting's origin and state after reboot remain open device-configuration questions. |

Android API references establish the supported interfaces; source inspection
establishes the host's filtering rule. Hardware observations will establish which
change resolves discovery on the recorded devices.

## Delivery sequence

### 1. Discoverability prototype

- Add **Add device** with a proposed 120-second pairing window, system confirmation,
  and visible pairing status. Use the duration returned by the system when showing
  the countdown.
- Request `BLUETOOTH_ADVERTISE` when needed on Android 12 and later, alongside the
  existing connection permission. The initial flow lets the host scan for the phone.
- Keep HID registration available during discovery and pairing. Record registration,
  discoverability, bonding, and connection as separate states.
- Capture the host's Classic inquiry result, including the received device class
  and available service information, with identifiers redacted before publication.
- Handle permission denial, cancelled system confirmation, Bluetooth being off,
  pairing-window expiry, and a retry initiated by the user.

**Exit criterion:** establish whether the host receives the phone's inquiry
response and whether its scan filter excludes that result. Record a screenshot,
the relevant state transitions, and the software versions.

### 2. Host discovery compatibility

Use the prototype evidence to select the smallest host-side adjustment:

- Retain the existing fast path for Peripheral-class devices and evaluate HID
  service information for additional Classic candidates. Service discovery through
  SDP can confirm the advertised HID capability where available.
- Evaluate a user-selected **Show other Classic devices** entry as a fallback when
  service information is incomplete. Pairing remains an explicit user action.
- Resolve device names and present selectable results through the host interface.
- Validate an existing keyboard or remote as well as the phone when changing the
  host's filtering behavior.

Track host compatibility work separately from the Android implementation and
document the exact host version or patch required by the tested configuration.
Keep the Android transport based on standard Bluetooth Classic HID APIs.

**Exit criterion:** the phone appears by name, the user selects it, and pairing
succeeds through the normal host and phone interfaces.

### 3. Session ownership and connection recovery

- Evaluate a `connectedDevice` foreground service as the owner of the HID manager,
  registration callback, and active connection. Start it from an explicit user action
  while the app is visible and satisfy the relevant platform permission requirements.
- Let the Activity observe session state. Preserve an active pairing or reading
  session when the Activity stops, with a notification that offers **Disconnect**
  and **Stop session**.
- Verify that the platform retains HID registration while the foreground service
  is active on each tested phone build.
- Preserve generation checks, operation timeouts, serialized key pulses, and final
  key release during disconnect and service shutdown.
- Handle Bluetooth changes, remote disconnection, service loss, and permission
  revocation with accurate state and an actionable recovery path.

This stage can begin alongside the prototype if system UI transitions interrupt
registration. Its full lifecycle checks precede completion of the milestone.

**Exit criterion:** pairing and a connected idle session survive a Settings visit,
return to the app, and a screen-lock interval on the recorded phone configuration.
After each transition, the on-screen controls work when the app is reopened.

### 4. Remembered host and one-tap reconnection

- Persist the last explicitly selected host locally and revalidate its bond when
  loading it. Offer **Reconnect** after registration becomes ready.
- Refresh bonded-device state after pairing and bond changes. Update the selection
  immediately after a successful connection initiated by the host.
- Handle a forgotten device and a stale saved selection through the pairing flow.
- Update privacy documentation to describe the saved device identifier and its
  retention. Keep the identifier local and exclude it from published diagnostics.

**Exit criterion:** reopening the app presents the saved host, and one tap restores
a usable connection. Automatic retry policy remains a later, separately validated
enhancement.

## System compatibility

The Android app uses the public SDK on phones with an available HID Device profile.
The runtime property change used on the rooted LineageOS test phone remains a
device-specific troubleshooting procedure. The plan keeps system configuration
work separate from ordinary app pairing and connection management.

Service-acquisition timeouts should describe the observed failure and provide
troubleshooting guidance. A specific cause, such as a disabled system profile or
another registered HID app, requires supporting evidence. Recheck the test phone's
profile state after reboot and record the result before claiming reboot recovery.

## Acceptance record to complete

All checks below are **pending** for the planned connection workflow. Earlier
successful address-based pairing remains the input-delivery baseline.

| Check | Acceptance target |
| --- | --- |
| First-time setup | Starting with pairing records cleared on both devices, select the phone by name, confirm pairing, connect, and turn pages using the phone and host interfaces. Complete three independent setup cycles. |
| Discovery evidence | Record phone HID registration, the system discoverability window, the host inquiry result, and the displayed scan entry. |
| Setup independence | Complete setup and reconnection through the product interfaces, with zero manual address entry, shell commands, or SSH steps. Diagnostic capture stays outside the user workflow. |
| Page input | After each setup cycle, complete at least 20 alternating Previous / Next presses with one page action per press and a released key state. |
| Manual reconnection | Complete ten Disconnect / Reconnect cycles with successful page input after each cycle. |
| Lifecycle | Keep a connected idle session during a Settings visit and at least one minute of screen lock; verify page controls after returning. |
| Bluetooth recovery | Toggle phone Bluetooth off and on, restore registration, and reconnect through the app. Record host Bluetooth or daemon restart behavior separately. |
| Reboot recovery | Record profile availability after reboot and confirm pairing retention and one-tap reconnection when the profile is available. |
| Interrupted setup | Permission denial, cancelled confirmation, expired discoverability, and host unavailability produce accurate status and a working retry path. |
| Regression | Run debug compilation, JVM tests, and Android Lint for code changes. Validate descriptor handling, key release, stale callbacks, and safe session shutdown. |

Each result should identify the app commit, phone build, host integration version,
procedure, and observed outcome. Publish useful screenshots and sanitized event
records using the existing validation-report format.

## Priority and scope

This work is the next planned **v0.2.0** milestone, ahead of volume-button controls
and configurable mappings. Its deliverable is discovery by name, interface-based
pairing, service-managed sessions, and one-tap reconnection on the documented
hardware configuration. Release planning follows the acceptance results.

Screen-off input capture, automatic reconnection policy, and gesture profiles have
their own later validation scope. The initial screen-lock check verifies session
retention and recovery of the visible controls.

## Primary references

- [Android device discovery and discoverability](https://developer.android.com/develop/connectivity/bluetooth/find-bluetooth-devices)
- [Android Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Android HID Device API and registration lifecycle](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice)
- [Android connected-device foreground service requirements](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device)
- [Tested host scan filter, version 3.17.5, lines 209–253](https://github.com/zampierilucas/kindle-hid-passthrough/blob/ba486596b212974ed1c7a701ab833f3f4adfb89a/kindle_hid_passthrough/scanner.py#L209-L253)
- [LineageOS HID service profile gate and process-importance checks, reviewed revision](https://github.com/LineageOS/android_packages_modules_Bluetooth/blob/52cfc06f1464df726c11b47b7ad764ebc5b8f0bf/android/app/src/com/android/bluetooth/hid/HidDeviceService.java)

References reviewed on 2026-11-16. Host source is linked for compatibility analysis.
