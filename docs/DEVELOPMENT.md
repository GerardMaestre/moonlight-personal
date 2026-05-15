# Desarrollo de Moonlight Personal

## Herramientas Requeridas

| Herramienta | Versión Mínima | Notas |
|------------|----------------|-------|
| Android Studio | Jellyfish (2024.1) | Soporte Compose + KMP |
| JDK | 17+ | Temurin recomendado |
| Android SDK | 34 (target) / 21 (min) | |
| Android NDK | 27.0.12077973 | Para compilar código nativo |
| Git | 2.30+ | Con soporte de submodules |

---

## Primer Setup

```bash
# 1. Clonar repo con submodules
git clone --recursive https://github.com/GerardMaestre/moonlight-personal.git

# 2. Abrir en Android Studio
# File → Open → seleccionar directorio moonlight-personal/

# 3. Esperar a que Gradle sync termine

# 4. Compilar para verificar
./gradlew app:assembleNonRootDebug
```

---

## Estructura de Carpetas

```
moonlight-personal/
├── shared/              # Kotlin Multiplatform — Compose UI + lógica
│   └── src/
│       ├── commonMain/  # Código compartido (agnóstico de plataforma)
│       ├── androidMain/ # Implementaciones Android (expect/actual)
│       ├── desktopMain/ # Implementaciones Desktop (expect/actual)
│       └── commonTest/  # Tests unitarios compartidos
├── app/                 # Aplicación Android
│   └── src/
│       ├── main/
│       │   ├── java/    # Código fuente (Java legacy + Kotlin)
│       │   ├── jni/     # Código nativo C (decoders)
│       │   └── res/     # Recursos Android (layouts, strings, drawables)
│       └── test/        # Tests unitarios Android
├── desktopApp/          # (Excluido Phase 1) App desktop
├── docs/                # Documentación
├── .github/             # CI/CD workflows
└── gradle/              # Gradle wrapper
```

---

## Variantes de Compilación

| Variante | Comando | Uso |
|----------|---------|-----|
| Debug (nonRoot) | `./gradlew app:assembleNonRootDebug` | Desarrollo diario |
| Release (nonRoot) | `./gradlew app:assembleNonRootRelease` | Distribución |
| Debug (root) | `./gradlew app:assembleRootDebug` | Dispositivos rooteados (API ≤25) |

### Debug vs Release

- **Debug**: con logs, sin minificación, applicationId suffix `.debug`
- **Release**: minificado con ProGuard, applicationId suffix `.unofficial`, requiere signing

---

## Flujo de Desarrollo

```
1. git checkout -b feature/mi-feature main
2. Hacer cambios
3. ./gradlew app:assembleNonRootDebug    # Compilar
4. ./gradlew :shared:test                 # Tests
5. Testear en emulador/device
6. git add . && git commit -m "feat: mi feature"
7. git push origin feature/mi-feature
8. Crear PR en GitHub
9. CI debe pasar → Review → Merge
```

---

## Debugging

### Logcat en Android Studio

1. **View → Tool Windows → Logcat**
2. Filtrar por tag: `Moonlight` o `Limelight`
3. Nivel: Debug para desarrollo, Error para producción

### Debugger

1. Poner breakpoints en el código
2. **Run → Debug** (Shift+F9)
3. Usar el inspector de variables y la consola de evaluación

### Network Debugging

- Los clientes HTTP (`UpSnapClient`, `RemoteScriptClient`) imprimen logs con `println`
- Para debugging avanzado, usar Charles Proxy o mitmproxy

---

## Testing

```bash
# Tests unitarios del módulo shared
./gradlew :shared:test

# Tests unitarios de la app Android
./gradlew :app:testNonRootDebugUnitTest

# Tests instrumentados (requiere emulador o device conectado)
./gradlew :app:connectedNonRootDebugAndroidTest

# Instalar directamente en device
./gradlew :app:installNonRootDebug
```

---

## Troubleshooting

### Gradle sync falla

```bash
# Limpiar y refrescar
./gradlew clean
./gradlew --refresh-dependencies

# Si el problema persiste, borrar caches
rm -rf .gradle/
rm -rf ~/.gradle/caches/
```

### NDK build falla

Verificar que el NDK 27.0.12077973 está instalado:
- Android Studio → Settings → Languages & Frameworks → Android SDK → SDK Tools
- Marcar "NDK (Side by side)" y la versión 27.x

### Emulador lento

- Usar imagen de sistema x86_64 (no ARM)
- Habilitar hardware acceleration (Hyper-V en Windows, KVM en Linux)
- Asignar más RAM al emulador (Android Studio → Device Manager → Edit)

### Compose preview no funciona

- Verificar que `compose.uiTooling` está en las dependencias
- Build → Clean Project → Rebuild
- File → Invalidate Caches → Restart

---

## Signing (Release)

### Generar keystore (una sola vez)

```bash
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias moonlight -storepass YOUR_PASSWORD
```

### Configurar en GitHub Secrets

1. Encodear keystore: `base64 release.keystore > keystore.b64`
2. GitHub → Settings → Secrets → Actions:
   - `SIGNING_KEY_STORE_BASE64` = contenido de keystore.b64
   - `SIGNING_KEY_ALIAS` = moonlight
   - `SIGNING_KEY_PASSWORD` = tu password
   - `SIGNING_STORE_PASSWORD` = tu password

> **⚠️ Nunca commitear el keystore ni passwords al repositorio.**
