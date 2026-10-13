# Contributing

Keep the app simple, standards-based, privacy-friendly, and independently maintainable.

## Scope

The initial milestone is manual Bluetooth HID input from the foreground Activity.
Do not add volume-key interception until the Android → host → reader path is verified.
Do not add accessibility, services, cloud features, or architecture frameworks without a demonstrated need.

## Code provenance

- Original contributions are licensed under this project's MIT License. By contributing,
  you confirm that you can license your contribution this way.
- Never copy, translate, or lightly rewrite code from a project without a clear compatible license.
- Public visibility on GitHub is not permission to copy.
- Reference official API documentation and protocol standards to implement behavior independently.
- Before adding third-party code/dependencies, explain source, license, MIT compatibility and
  attribution obligations to the maintainer, then update DEPENDENCIES.md and retained notices.
- Preserve upstream license headers in any explicitly licensed vendored component.
- Review runtime transitive dependencies and build/test components separately; do not treat the
  entire Android SDK or toolchain as project-owned MIT code.

## Development

Use Kotlin and one app module. Keep Bluetooth lifecycle logic outside the UI.
Use typed HID usages, named timings, narrow exception handling and actionable error messages.
Do not log device identifiers or user content. No INTERNET permission, telemetry or analytics.

Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
Test reports and timing without Bluetooth hardware; record real hardware results in
docs/DEVICE_TESTING.md. Compilation is not evidence of Bluetooth interoperability.

Use small commits such as `feat: add HID keyboard reports` or `docs: explain device testing`.
Do not commit local SDK paths, private keys, build caches, device logs or personal device addresses.
