# Manual device validation

Status: **not run**. No Android device was attached when the initial skeleton was built.

## Record the environment

| Item | Result |
| --- | --- |
| Phone model / Android version / build | Not recorded |
| App version / Git commit | 0.1.0-dev; record tested commit |
| Target model / firmware | Kindle Paperwhite 6; firmware not recorded |
| Kindle HID support / forwarding setup | Not verified |
| KOReader version / active key mapping | Not recorded |
| Pairing direction and steps | Not recorded |

Do not publish Bluetooth addresses or account/serial identifiers in reports.

## Install and register

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n org.androidpageturner/.MainActivity
adb logcat -s HidManager MainActivity
```

1. Turn Bluetooth on and complete pairing using system UI on the phone/host.
2. Open the app. On Android 12+, tap Refresh / Register HID and allow Nearby devices.
3. Expect `HID service available`, `Register HID app`, `HID app registered` in Logcat.
4. If unsupported/timeout/error is shown, record the exact status and Android version. A timeout
   alone does not prove the profile is unsupported. Close other HID apps, check Bluetooth and retry.

For first diagnosis, a computer with a simple key-event viewer or a text editor is useful:
Left / Right should move a cursor once. This isolates phone HID behavior before Kindle forwarding.
Testing a computer does not replace the requested Kindle + KOReader test.

## Connect and verify input

1. Pick the paired host and tap Connect. Wait for Connected, not merely a log saying Connect device.
2. Keep the phone app visible. Open a book on the host.
3. Tap Next once; check one Right Arrow and one page advance.
4. Tap Previous once; check one Left Arrow and one page back.
5. Repeat at least 20 alternating presses. Check for duplicates or automatic repeats after release.
6. Press while another pulse is active: the UI is disabled briefly; it does not queue extra pages.
7. Disconnect, wait for Disconnected, reconnect and test again.

## Failure and lifecycle checks

- Deny Nearby devices permission, including permanent denial; UI explains how to retry/enable it.
- Test with no bonded devices and no selection; Connect and page buttons must be disabled.
- Connect to a paired device that is not a HID host: failure/timeout must be visible.
- Turn the host off while connected; page buttons must become disabled after the callback.
- Turn phone Bluetooth off, then on; registration can recover and connection remains manual.
- Press Home/open Settings/rotate/lock the phone during a pulse: verify no held/repeating host key.
- Reopen the app: HID should register again; manually reconnect and test.
- Close another HID app before testing registration conflicts.
- If the host requests keyboard LEDs or boot protocol, verify normal key behavior afterward.

## Kindle Paperwhite 6 → KOReader

Verify that the Kindle's Bluetooth stack accepts keyboard HID and forwards events to KOReader.
If a computer receives arrow keys but Kindle does not, investigate Kindle-side HID support first.
If Kindle receives input but KOReader does not turn pages, investigate reader bindings/focus.
Do not change the Android app to use a Kindle private protocol to work around an unverified host path.

App settings currently provide no in-app discovery/pairing. Some hosts need the keyboard SDP
present at first pairing; record if the system-pairing-then-HID-registration sequence fails.
If so, design a small, permission-reviewed pairing enhancement after the basic profile is confirmed.

## Acceptance gate

Record actual results below; leave untested items pending.

| Check | Result |
| --- | --- |
| HID Device Profile available | Pending |
| Registration callback true | Pending |
| Paired host connected callback | Pending |
| Left / Right accepted on a computer | Pending |
| Kindle HID input received | Pending |
| KOReader Previous / Next once per tap | Pending |
| No held key after pulse / background / disconnect | Pending |
| Manual disconnect and reconnect | Pending |
| Permission denial and Bluetooth-off recovery | Pending |

**Do not implement volume-key interception until the page-turning path is verified.**
For the next milestone, foreground dispatchKeyEvent will consume both DOWN and UP for volume keys,
ignore repeatCount > 0, and emit one HID pulse; no AccessibilityService is planned for that milestone.
