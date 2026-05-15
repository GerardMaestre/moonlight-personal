# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GitHub Actions CI/CD pipeline (`build.yml` + `release.yml`)
- Dependabot configuration for automated dependency updates
- `docs/ARCHITECTURE.md` — Complete architecture documentation
- `docs/CONTRIBUTING.md` — Contribution guidelines
- `docs/DEVELOPMENT.md` — Development setup and workflow guide
- `CHANGELOG.md` — This file

### Changed
- **README.md** — Complete rewrite with accurate feature descriptions
  - Removed false claims (iPhone-style UI, glassmorphism, Dynamic Network Profiles)
  - Added accurate project status table
  - Added architecture overview
  - Added proper build instructions
  - Added roadmap
- **settings.gradle.kts** — Removed `:desktopApp` from Phase 1 build, added `dependencyResolutionManagement`
- **.gitignore** — Updated with comprehensive exclusion rules

### Removed
- `LuaScripts/` — Orphaned Wireshark dissector scripts (no references in codebase)
- `appveyor.yml` — Legacy CI configuration (replaced by GitHub Actions)
- `fastlane/metadata/` — Empty Play Store metadata (no real configuration)

### Fixed
- Architecture mismatch between README claims and actual codebase
- Missing CI/CD pipeline

---

## Previous History

This project is a fork of [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) with custom additions by Gerard Maestre. Prior to this changelog, changes were tracked via Git commits and pull requests.

### Notable Prior Additions
- Jetpack Compose dashboard (PremiumDashboardActivity)
- UpSnap WoL integration (PowerControlScreen)
- Remote Immich server control (PhotoServerScreen)
- Kotlin Multiplatform shared UI module
- Material 3 dark theme (MoonlightTheme)
- Stream Deck remote script execution
