# Moonlight Personal — Custom Edition 🌙

[![Android CI](https://github.com/GerardMaestre/moonlight-personal/actions/workflows/build.yml/badge.svg)](https://github.com/GerardMaestre/moonlight-personal/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE.txt)

**Moonlight Personal** is a customized fork of the [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) game streaming client, with additional home automation features and a modern Jetpack Compose dashboard.

---

## 📋 Contents

1. [Features](#-features)
2. [Project Status](#-project-status)
3. [Build & Installation](#️-build--installation)
4. [Architecture](#-architecture)
5. [Contributing](#-contributing)
6. [License](#️-license)

---

## ✨ Features

### 🎮 Game Streaming (Moonlight Core)
- **GameStream / Sunshine Protocol:** Full-featured game streaming from NVIDIA GameStream or [Sunshine](https://github.com/LizardByte/Sunshine) hosts
- **Codec Support:** H.264, HEVC, and AV1 video decoding via Android MediaCodec
- **Low Latency:** Hardware-accelerated video decoding with configurable bitrate and frame pacing
- **Input Support:** Touch, gamepad, mouse, and keyboard input forwarding

### 🎨 Modern Dashboard UI
- **Jetpack Compose:** Custom dashboard ("Panel de Control") built with Jetpack Compose and Material Design 3
- **Dark Theme:** Custom dark color palette designed for OLED displays
- **Responsive Layout:** Adapts to phones, tablets, and landscape orientation
- **Shared UI:** Screens are built in a Kotlin Multiplatform `shared/` module

### 🌐 Connectivity
- **Automatic Discovery:** mDNS-based PC discovery on the local network (inherited from Moonlight)
- **Direct IP:** Connect by static IP address when autodiscovery doesn't work
- **Wake-on-LAN (UpSnap):** Integrated with [UpSnap](https://github.com/seriousm4x/UpSnap) WoL API for remote PC wake
- **Wake-on-LAN (UDP):** Standard Magic Packet WoL for same-subnet scenarios
- **Manual Codec/Bitrate Selection:** Choose codec and bitrate from the settings screen

### 🏠 Home Automation
- **UpSnap Integration:** Configure and trigger WoL from a beautiful power control panel
- **Remote Script Execution:** Trigger batch scripts on a Stream Deck PC via authenticated HTTP API
- **Immich Photo Server:** Start/stop an Immich Docker stack remotely from the mobile app

---

## 📊 Project Status

| Feature | Status | Notes |
|---------|--------|-------|
| Android Streaming Client | ✅ Functional | Core inherited from Moonlight Android |
| mDNS Discovery | ✅ Functional | Via jmDNS library |
| H.264 / HEVC Streaming | ✅ Functional | MediaCodec hardware decoding |
| Compose Dashboard UI | ✅ Functional | 6 screens in shared/ module |
| Wake-on-LAN (UpSnap) | ✅ Functional | HTTP API + JWT auth |
| Wake-on-LAN (UDP) | ✅ Functional | Standard Magic Packet |
| Direct IP Connection | ✅ Functional | Manual PC addition |
| Immich Remote Control | ✅ Functional | Via Stream Deck HTTP API |
| Jetpack Compose Migration | 🟡 In Progress | Dashboard is Compose; legacy screens are still Java |
| Desktop Client | ❌ Removed (Phase 1) | Was an Immich server manager, not a streaming client |
| Dynamic Network Profiles | ❌ Not Implemented | Data model exists, but profiles are not applied to streams |

---

## 🛠️ Build & Installation

### Prerequisites

- **Android Studio** Jellyfish (2024.1) or later
- **JDK 17+**
- **Android SDK 34** (target) / **SDK 21** (minimum)
- **Android NDK 27** (for native code)

### Build from Source

```bash
# Clone with submodules
git clone --recursive https://github.com/GerardMaestre/moonlight-personal.git
cd moonlight-personal

# Build debug APK
./gradlew app:assembleNonRootDebug

# Build release APK (requires signing config)
./gradlew app:assembleNonRootRelease
```

### Download Prebuilt APK

Signed APKs are published on the [Releases](https://github.com/GerardMaestre/moonlight-personal/releases) page.

> **Note:** Never distribute debug APKs. Always use signed release builds for production use.

---

## 🏗 Architecture

```
moonlight-personal/
├── shared/                    # Kotlin Multiplatform shared module
│   └── src/
│       ├── commonMain/        # Platform-agnostic code
│       │   ├── model/         # ComputerInfo, GameInfo, NetworkProfile
│       │   ├── network/       # UpSnapClient, RemoteScriptClient
│       │   ├── platform/      # PlatformActions interface, PhotoServerContract
│       │   └── ui/
│       │       ├── screens/   # Compose screens (Dashboard, PowerControl, etc.)
│       │       ├── components/# Reusable Compose components
│       │       └── theme/     # MoonlightTheme (dark Material 3)
│       ├── androidMain/       # Android expect/actual implementations
│       └── desktopMain/       # Desktop expect/actual implementations
├── app/                       # Android application
│   └── src/main/
│       ├── java/              # Legacy Java code (Moonlight core + custom Kotlin)
│       │   └── com/limelight/
│       │       ├── ui/premium/  # Compose Activity + ViewModel
│       │       ├── nvstream/    # GameStream protocol (inherited)
│       │       ├── computers/   # PC discovery & management
│       │       └── binding/     # Platform bindings (input, video, audio)
│       ├── jni/               # Native C code (video decoding)
│       └── res/               # Android resources
├── docs/                      # Documentation
└── .github/workflows/         # CI/CD pipelines
```

For detailed architecture documentation, see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## 🤝 Contributing

See [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) for contribution guidelines.

---

## 🚀 Roadmap

### Phase 1 (Current)
- [x] Architecture audit
- [x] Remove dead code (LuaScripts, appveyor, fastlane)
- [x] Fix README to match reality
- [x] CI/CD with GitHub Actions
- [ ] Automated signed releases

### Phase 2 (Next)
- [ ] Extract hardcoded credentials to settings
- [ ] Migrate Java custom code to Kotlin
- [ ] Unify WoL implementations
- [ ] Add unit test coverage (>70%)

### Phase 3 (Future)
- [ ] Desktop streaming client (Compose Desktop)
- [ ] Proper expect/actual abstractions (Logger, Storage, Network)
- [ ] Dependency injection (Hilt)

---

## 👨‍💻 Author

Customized and maintained by **Gerard Maestre**.

*Original Moonlight Authors:*
- Cameron Gutman
- Diego Waxemberg
- Aaron Neyer
- Andrew Hennessy

---

## ⚖️ License

This project is licensed under the **GPLv3 License**. See [LICENSE.txt](LICENSE.txt) for details.

Based on the original [Moonlight Android](https://github.com/moonlight-stream/moonlight-android).
All custom changes and additions by Gerard Maestre.
