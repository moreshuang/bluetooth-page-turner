# Changelog

Notable project changes are recorded here. The current development version is `0.1.0-dev`.

## Unreleased

### Added

- Kotlin Android application with MIT License and documented dependency attribution.
- Foreground Bluetooth HID registration and paired-host selection.
- Standard keyboard descriptor, Previous / Next controls, and timed key release.
- Manual connection management, lifecycle cleanup, status messages, and session timeouts.
- JVM tests for descriptor structure, keyboard reports, key-pulse timing, and UI state eligibility.
- Build instructions, permission reference, privacy documentation, and a hardware validation guide.

### Documentation

- Standardized project, contributor, technical, and validation documentation.
- Focused the project overview on supported functionality and development milestones.

### Fixed

- Wait for the HID registration callback when a phone returns `false` from `registerApp`
  while completing registration asynchronously. Verified on Xiaomi 13 with Android 13.
- Distinguish registration failure from callback timeout and provide neutral diagnostic messages.

### Validation status

Compilation, 15 JVM unit tests, and Android Lint passed during initial build verification.
HID registration, refresh, and reopening passed on Xiaomi 13 with Android 13 on 2026-10-14.
Hardware validation of host connection and page navigation is pending.
Volume-button controls, configurable mappings, and screen-off operation are planned milestones.
