# Changelog

## [Unreleased] — 0.1.0-dev

### Added

- Independent Kotlin Android app skeleton with MIT License and dependency provenance.
- Foreground Bluetooth HID registration and system-paired host selection.
- Standard keyboard descriptor, LEFT / RIGHT page buttons and timed key release.
- Manual connect/disconnect, lifecycle cleanup, error feedback and session timeout handling.
- Pure JVM tests for descriptor size, keyboard reports, pulse timing and UI state eligibility.
- Build, permissions, privacy and manual device-test documentation.

### Verification boundary

- Bluetooth HID support and Kindle/KOReader interoperability have not been tested on hardware.
- Physical volume keys, custom mappings, background use and screen-off support are not implemented.
