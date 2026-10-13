# Contributing

Thank you for contributing to Bluetooth Page Turner. The project prioritizes simple
implementation, standard Bluetooth HID behavior, clear documentation, and local operation.

## Issues and proposals

Use [GitHub Issues](https://github.com/moreshuang/bluetooth-page-turner/issues) to report
bugs, share device results, or propose changes. For larger changes, describe the use case
and approach in an issue before preparing a pull request.

A useful bug report includes:

- App version or commit, phone model, and Android version.
- Host category, firmware, and reading application version where relevant.
- Steps to reproduce, expected behavior, and observed behavior.
- Relevant diagnostic output with personal and device identifiers redacted.

Use [the device testing guide](docs/DEVICE_TESTING.md) for compatibility reports.

## Development scope

The current milestone is foreground HID registration, connection, and on-screen page
navigation. Hardware validation of this workflow precedes volume-button controls.
Subsequent milestones are listed in the [roadmap](README.md#roadmap).

Keep changes focused on a demonstrated use case. Use Kotlin, a single app module, small
functions, and a clear separation between Bluetooth lifecycle management and UI code.
Prefer platform APIs and a minimal dependency set.

## Licensing and attribution

Contributions to project code use the MIT License. Contributors must hold the rights
required to submit their work under that license.

Implement behavior using official API documentation, protocol specifications, and clearly
licensed materials. Each third-party dependency or incorporated source must have an
explicit license compatible with its intended use in the project.

Before introducing third-party material, provide the maintainer with:

- Source and exact version.
- License and compatibility with MIT-licensed project code.
- Required copyright, attribution, and redistribution notices.
- Intended scope: reference material, dependency, or incorporated source.

Update [DEPENDENCIES.md](DEPENDENCIES.md) and retain applicable upstream headers and
license texts. Review runtime transitive dependencies alongside direct dependencies;
review build and test tools according to their distribution terms.

## Quality checks

Use named HID key usages and timing constants, handle expected exceptions explicitly,
and provide actionable status messages. Diagnostic logging should describe operations
while protecting device identifiers and user content.

For code changes, run:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Use JVM tests for protocol encoding and timing. Validate Bluetooth framework behavior on
hardware and record the environment and observed results. For documentation changes,
check relative links, formatting, and consistency with the current implementation.

Keep SDK paths, credentials, build caches, and device logs in local files excluded from Git.

## Documentation style

Write for users and contributors with concise, factual descriptions of project capabilities.
Prefer affirmative wording, and distinguish implemented behavior, validation status, and
planned work. Keep development-environment details in the relevant technical records.

Mention other projects where needed for dependency attribution, primary references, or
reproducible device-test configurations. Keep the project overview focused on this
application's purpose, usage, and maintenance. Preserve required license notices and
accurate compatibility information when editing documentation.

## Pull requests and commits

Submit focused pull requests that explain the user-visible change, relevant implementation
choices, and validation performed. Include device-test results for changes affecting HID behavior.

Use small, descriptive commits, for example:

```text
feat: add HID keyboard reports
fix: release keyboard state before disconnect
docs: clarify device testing steps
```

Document validation that remains pending so maintainers can assess the change accurately.
