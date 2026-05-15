# Moonlight Personal - Custom Edition 🌙🚀

[![Custom Build](https://img.shields.io/badge/Build-Custom_Edition-blueviolet?style=for-the-badge&logo=android)](https://github.com/GerardMaestre/moonlight-personal)
[![License](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE.txt)

**Moonlight Personal** is a high-performance, customized fork of the Moonlight Android client. This version is optimized for a premium user experience with advanced features not found in the standard build.

---

## ✨ Key Custom Features

### 📱 iPhone-Style Premium UI
- **Jetpack Compose Native:** Completely redesigned interface using modern Jetpack Compose for buttery-smooth 60fps animations.
- **Minimalist Aesthetic:** Clean, "iPhone-style" design with glassmorphism effects and curated color palettes.
- **Fluid Transitions:** Hardware-accelerated screen transitions for a tactile, high-end feel.

### 🌐 Advanced Connectivity
- **Direct IP Linking:** Bypass network discovery lag with pre-configured direct IP connectivity for instant sessions.
- **HTTP Wake-on-LAN:** Integrated automated WoL via HTTP requests, allowing you to wake your gaming PC with a single tap from anywhere.
- **Dynamic Network Profiles:** Quick-access profiles that automatically optimize bitrate, codec (HEVC/H.264), and frame pacing based on your connection (5G, Wi-Fi 6, or Remote).

---

## 🛠️ Build & Installation

### Download the APK
You can find the latest compiled version in the `app/build/outputs/apk/nonRoot/debug/` directory or download it from the [Releases](https://github.com/GerardMaestre/moonlight-personal/releases) section.

### Local Compilation
1. Clone this repository:
   ```bash
   git clone --recursive https://github.com/GerardMaestre/moonlight-personal.git
   ```
2. Open in **Android Studio**.
3. Build the `nonRoot` variant for standard device deployment.

---


## 🎨 UI Architecture Reference

For non-streaming UI architecture, reusable UI states/components, and migration/motion guidelines, see:

- [`docs/ui/non-streaming-ui-guidelines.md`](docs/ui/non-streaming-ui-guidelines.md)

---

## 👨‍💻 Author & Contributions

Customized and maintained by **Gerard Maestre**.

*Original Moonlight Authors:*
- Cameron Gutman
- Diego Waxemberg
- Aaron Neyer
- Andrew Hennessy

---

## ⚖️ License
This project is licensed under the GPLv3 License. See [LICENSE.txt](LICENSE.txt) for details. Based on the original [Moonlight Android](https://github.com/moonlight-stream/moonlight-android).

## 🪟 Configuración de servidor en Windows (desktopMain)

### Objetivo
- La app de escritorio ahora soporta ejecución **headless** del servidor de fotos (`--server`) sin abrir UI.
- La UI puede iniciar/detener el servidor y recibir estado (`running/failed`) en pantalla.
- Se publica endpoint `/health` y logging local para verificar que el proceso sigue vivo incluso con sesión bloqueada.

### Ejecución manual
1. Iniciar UI normal:
   ```bash
   ./gradlew :desktopApp:run
   ```
2. Iniciar solo servidor (sin UI):
   ```bash
   ./gradlew :desktopApp:run --args="--server"
   ```

### Autoarranque (Task Scheduler recomendado)
- En Windows, el modo `--server` intenta registrar tarea `MoonlightPhotoServer` usando `schtasks`.
- Para ejecutar aunque la sesión esté cerrada, configure la tarea en el Programador de tareas con:
  - **Run whether user is logged on or not**
  - **Run with highest privileges** (si necesita puertos/permisos elevados)
- Trigger sugerido: `At log on` o `At startup` según su política.

### Logs y healthcheck
- Archivo de log local:
  - `%USERPROFILE%\\.moonlight\\photo-server.log`
- Endpoint de salud:
  - `http://<host>:<puerto>/health`
- La pantalla “Servidor de Fotos” muestra:
  - último resultado de inicio
  - healthcheck periódico
  - logs recientes

### Limitaciones (UAC / permisos / sesión bloqueada)
- Registrar tareas en `Task Scheduler` puede requerir permisos de administrador (UAC).
- Si se selecciona “Run whether user is logged on or not”, el proceso corre en contexto no interactivo (sin UI).
- Reglas de firewall pueden bloquear puertos aunque el proceso esté activo.
- Credenciales de la tarea (password expirado/cambiado) pueden causar fallo en arranque en background.
