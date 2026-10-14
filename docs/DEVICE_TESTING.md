# Device testing

Hardware validation status: **partial** for `0.1.0-dev`. HID registration, refresh, and
reopening passed on Xiaomi 13 / Android 13 on 2026-10-14. Host input validation is pending.

This guide records the Bluetooth HID connection and page-navigation workflow needed for
the first release. Follow-up volume-button controls depend on successful validation of
this workflow.

## Environment record

Include the following details in a compatibility report:

| Item | Record |
| --- | --- |
| Phone | Model, Android version, and build |
| App | Version and tested Git commit |
| Host | Model, operating system, and firmware |
| Input integration | HID support and host-side event forwarding |
| Reading application | Version, focus, and active key mapping |
| Pairing | Direction and exact steps |

Redact Bluetooth addresses, account identifiers, and serial numbers from shared reports.

## Install and register

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n org.androidpageturner/.MainActivity
adb logcat -s HidManager MainActivity
```

1. Enable Bluetooth and complete pairing through the phone/host system settings.
2. Open the app. On Android 12 or later, select **Refresh / Register HID** and allow
   Nearby devices access.
3. Check for `HID service available`, `Register HID app`, and `HID app registered` in Logcat.
4. Record the UI status and Android version for service or registration errors. Retry after
   checking Bluetooth and any competing HID registration. A timeout calls for further
   diagnosis of profile availability.

A computer with a text editor or key-event viewer can help establish initial input delivery:
Left and Right should each move the cursor once. Validate the intended reading host after
this baseline check.

## Connect and navigate

1. Select the paired host and choose **Connect**. Wait for the **Connected** status.
2. Keep the phone app visible and open a document on the host.
3. Press **Next** once; verify one Right Arrow event and one page advance.
4. Press **Previous** once; verify one Left Arrow event and one page back.
5. Repeat at least 20 alternating presses, checking event counts and release behavior.
6. Verify that controls are briefly disabled during each active key pulse.
7. Choose **Disconnect**, wait for **Disconnected**, reconnect, and repeat the input check.

## Error handling and lifecycle

| Scenario | Expected check |
| --- | --- |
| Nearby devices permission denied | Clear permission guidance and a working retry/settings path |
| Empty paired-device list or cleared selection | Connection and page controls remain disabled |
| Paired host lacks keyboard HID support | Visible connection error or timeout |
| Host powered off during connection | State updates and page controls become disabled |
| Phone Bluetooth toggled off and on | Registration recovers; host connection remains user initiated |
| Home, Settings, rotation, or screen lock during a pulse | Host key state is released and the session is cleaned up |
| App reopened | HID registers again and manual connection succeeds |
| Competing HID application active | Registration status and recovery guidance are accurate |
| Host requests LED reports or boot protocol | Page controls continue to send the expected keys |

## E-reader validation

The initial e-reader test configuration is Kindle Paperwhite 6 with KOReader.
Record firmware, reader version, keyboard input integration, and key mappings for this
configuration.

Validate each step independently:

1. The host accepts Bluetooth keyboard HID input.
2. Host-side input handling forwards navigation events to the reader.
3. The reader interprets Left and Right as the expected page actions.

Use a successful computer input test to isolate Android report behavior, then examine host
input forwarding and reader bindings as needed. Maintain standard keyboard reports across
host configurations.

Some hosts require keyboard SDP information during initial pairing. Record the complete
pairing sequence, including app foreground state. Any proposed pairing enhancement should
address the observed behavior and its permission requirements.

## Acceptance record

Record the environment, tested commit, observed result, and supporting evidence for each
check. Mark outstanding checks as pending.

| Check | Result |
| --- | --- |
| HID Device Profile available | Passed on Xiaomi 13 / Android 13, 2026-10-14 |
| Registration callback true | Passed on Xiaomi 13 / Android 13, 2026-10-14 |
| Paired-host connection callback | Pending |
| Left / Right input received on a computer | Pending |
| E-reader HID input received | Pending |
| Reader navigates once per button press | Pending |
| Key release after pulse, backgrounding, and disconnect | Pending |
| Manual disconnect and reconnect | Pending |
| Permission guidance and Bluetooth recovery | Pending |

After this workflow passes, validate foreground volume-button handling. The planned
implementation uses `dispatchKeyEvent`, consumes DOWN and UP events, and emits one HID
pulse per initial press while suppressing repeated key-down events.
