# Arquitectura de Moonlight Personal

## Visión General

Moonlight Personal es un cliente de game streaming Android basado en el protocolo GameStream de NVIDIA / [Sunshine](https://github.com/LizardByte/Sunshine). Es un fork personalizado de [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) con funcionalidades adicionales de automatización del hogar.

## Estructura Modular

```
moonlight-personal/
├── shared/              # Kotlin Multiplatform — UI y lógica compartida
├── app/                 # Aplicación Android
├── desktopApp/          # (Excluido Phase 1) Gestor de Immich desktop
├── docs/                # Documentación
└── .github/workflows/   # CI/CD pipelines
```

---

## shared/ — Kotlin Multiplatform

Módulo compartido que contiene UI Compose y lógica de negocio reutilizable.

### Estructura

```
shared/src/
├── commonMain/kotlin/com/limelight/shared/
│   ├── model/              # Modelos de datos
│   │   ├── ComputerInfo.kt   # Representación de PC gaming
│   │   ├── GameInfo.kt       # Representación de juego
│   │   └── NetworkProfile.kt # Perfiles de red (data-only, no aplicado aún)
│   ├── network/            # Clientes de red
│   │   ├── UpSnapClient.kt       # Cliente API UpSnap (WoL via HTTP)
│   │   ├── RemoteScriptClient.kt # Ejecutor de scripts remotos
│   │   └── UpSnapUrlValidator.kt # Validación de URLs
│   ├── platform/           # Abstracciones de plataforma
│   │   ├── Platform.kt           # expect/actual + PlatformActions interface
│   │   └── PhotoServerContract.kt # Estado y acciones del servidor de fotos
│   └── ui/                 # Interfaz de usuario
│       ├── screens/
│       │   ├── AppController.kt      # Controlador central de estado
│       │   ├── AppNavigation.kt      # Navegación basada en stack
│       │   ├── DashboardScreen.kt    # Lista de PCs gaming
│       │   ├── DashboardState.kt     # Estado del dashboard
│       │   ├── MainMenuScreen.kt     # Menú principal (3 tarjetas)
│       │   ├── GameListScreen.kt     # Grid de juegos por PC
│       │   ├── PowerControlScreen.kt # Panel UpSnap WoL
│       │   └── PhotoServerScreen.kt  # Control de servidor Immich
│       ├── components/
│       │   ├── PcCard.kt             # Tarjeta de PC
│       │   ├── NetworkProfileCard.kt # Tarjeta de perfil de red
│       │   └── BottomNavBar.kt       # Barra de navegación inferior
│       └── theme/
│           └── MoonlightTheme.kt     # Tema oscuro Material 3
├── androidMain/kotlin/com/limelight/shared/platform/
│   └── Platform.android.kt          # actual fun getPlatformName() = "Android"
├── desktopMain/kotlin/com/limelight/shared/platform/
│   └── Platform.desktop.kt          # actual fun getPlatformName() = "Desktop"
└── commonTest/kotlin/com/limelight/shared/
    ├── platform/PhotoServerStateTest.kt
    └── ui/screens/AppControllerTest.kt
```

### Principios

1. **UI en shared/** — Todas las pantallas Compose están en `commonMain/`
2. **Estado observable** — `DashboardState`, `PowerControlState`, `PhotoServerState` usan `mutableStateOf`
3. **PlatformActions** — Interface que cada plataforma implementa para acciones específicas (abrir settings, WoL, pairing)
4. **AppController** — Fuente única de verdad para navegación y estado

---

## app/ — Aplicación Android

### Código Personalizado (Kotlin)

```
app/src/main/java/com/limelight/
├── ui/premium/
│   ├── PremiumDashboardActivity.kt   # Activity principal (Compose)
│   ├── PremiumDashboardViewModel.kt  # ViewModel — bridge a ComputerManagerService
│   ├── AndroidPhotoServerManager.kt  # Gestor de servidor de fotos (Android)
│   └── UpSnapConfig.kt              # SharedPreferences para credenciales UpSnap
└── utils/
    └── LanAddressResolver.kt        # Obtener IP local LAN
```

### Código Heredado (Java — Moonlight Core)

```
app/src/main/java/com/limelight/
├── nvstream/          # Protocolo GameStream
│   ├── NvConnection.java         # Conexión al servidor
│   ├── StreamConfiguration.java  # Config de stream (resolución, codec, bitrate)
│   ├── http/                     # HTTP API (NvHTTP, PairingManager)
│   ├── wol/WakeOnLanSender.java  # WoL UDP estándar
│   ├── mdns/                     # Descubrimiento mDNS
│   ├── input/                    # Paquetes de input (controller, keyboard, mouse)
│   ├── jni/MoonBridge.java       # Bridge JNI a código nativo
│   └── av/                       # Audio/Video renderers
├── computers/         # Gestión de PCs
│   ├── ComputerManagerService.java  # Service de descubrimiento
│   └── ComputerDatabaseManager.java # Base de datos SQLite
├── binding/           # Platform bindings
│   ├── input/ControllerHandler.java
│   └── video/MediaCodecDecoderRenderer.java
├── preferences/       # Pantalla de ajustes
│   ├── StreamSettings.java       # Settings Activity
│   └── PreferenceConfiguration.java
├── grid/              # Adaptadores de grid (apps, PCs)
└── utils/             # Utilidades (dialogs, helpers)
```

---

## Flujo de Datos

```
┌──────────────────────┐
│  PremiumDashboard     │
│  Activity (Compose)   │
│  ┌──────────────────┐ │
│  │ PremiumDashboard  │ │
│  │ ViewModel         │ │
│  └────────┬─────────┘ │
└───────────┼───────────┘
            │ creates & owns
            ▼
┌──────────────────────┐
│  AppController       │ ← shared/commonMain
│  ├─ DashboardState   │
│  ├─ AppNavigation    │
│  ├─ PowerControlState│
│  └─ PhotoServerState │
└───────────┬──────────┘
            │ feeds
            ▼
┌──────────────────────────────────┐
│  Compose Screens (shared/)       │
│  ├─ MainMenuScreen              │
│  ├─ DashboardScreen             │
│  ├─ GameListScreen              │
│  ├─ PowerControlScreen          │
│  └─ PhotoServerScreen           │
└──────────────────────────────────┘
```

### Descubrimiento de PCs

```
ComputerManagerService.java (Android Service)
    │ polls network via mDNS
    ▼
ComputerManagerListener.notifyComputerUpdated(ComputerDetails)
    │
    ▼ (in PremiumDashboardViewModel)
Converts ComputerDetails → ComputerInfo (shared model)
    │
    ▼
DashboardState.updateComputer(ComputerInfo)
    │
    ▼ (Compose recomposition)
DashboardScreen re-renders with new PC list
```

---

## Reglas de Arquitectura

1. **Lógica de negocio en shared/commonMain** — Discovery models, WoL config, streaming profiles
2. **expect/actual solo para plataforma-específico** — Actualmente solo `getPlatformName()`
3. **UI Compose en shared/** — Todas las pantallas nuevas van en `shared/ui/screens/`
4. **No duplicar lógica** — Si algo existe en shared/, no reimplementar en app/
5. **Java legacy: no agregar más** — Nuevas features siempre en Kotlin
6. **PlatformActions para callbacks** — Cada plataforma implementa la interface

## Deuda Técnica Conocida

| Issue | Severidad | Descripción |
|-------|-----------|-------------|
| `java.net.*` en commonMain | ⚠️ Media | `UpSnapClient` y `RemoteScriptClient` usan clases Java. Funciona porque ambos targets son JVM, pero no es truly multiplatform. |
| NetworkProfiles no aplicados | 🟡 Baja | Los perfiles existen pero `onApplyNetworkProfile()` es no-op. |
| WoL duplicado | ⚠️ Media | 3 implementaciones: `WakeOnLanSender.java`, `Main.kt` desktop, y `UpSnapClient`. |
| Credenciales hardcodeadas | 🔴 Alta | IPs y tokens en `PremiumDashboardActivity.kt`. |
| Sin DI | 🟡 Baja | Dependencias instanciadas manualmente. |
