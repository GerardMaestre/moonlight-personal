# Contribuyendo a Moonlight Personal

¡Gracias por tu interés en contribuir! Aquí tienes las pautas para hacerlo de forma efectiva.

## Setup del Entorno

```bash
# Clonar con submodules
git clone --recursive https://github.com/GerardMaestre/moonlight-personal.git
cd moonlight-personal

# Compilar para verificar que todo funciona
./gradlew app:assembleNonRootDebug
```

### Requisitos

- **Android Studio** Jellyfish (2024.1) o superior
- **JDK 17+**
- **Android SDK 34** + **NDK 27**

---

## Branching Model

| Rama | Propósito |
|------|-----------|
| `main` | Producción — siempre compilable y estable |
| `develop` | Integración diaria (cuando exista) |
| `feature/xxx` | Nuevas features (branched de main/develop) |
| `fix/xxx` | Bugfixes (branched de main/develop) |
| `refactor/xxx` | Refactorizaciones de arquitectura |

### Crear una rama

```bash
git checkout main
git pull origin main
git checkout -b feature/mi-nueva-feature
```

---

## Workflow de Pull Request

### 1. Antes de hacer PR

- [ ] Compila sin errores: `./gradlew app:assembleNonRootDebug`
- [ ] Tests pasan: `./gradlew :shared:test`
- [ ] Código formateado correctamente

### 2. Checklist del PR

- [ ] ¿El branch está actualizado con `main`?
- [ ] ¿Se han añadido tests para la nueva funcionalidad?
- [ ] ¿Se ha actualizado la documentación si es necesario?
- [ ] ¿El cambio afecta a ambas plataformas? Si sí, ¿está la lógica en `shared/commonMain/`?
- [ ] ¿Se han externalizado strings (sin hardcoded text en UI)?

### 3. Convención de commits

Usamos [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add UpSnap device selection UI
fix: resolve crash on PC discovery timeout
refactor: extract WoL logic to shared module
docs: update architecture diagram
chore: update Gradle wrapper to 8.5
```

---

## Estilo de Código

### Kotlin

- Sigue la [Kotlin Coding Convention](https://kotlinlang.org/docs/coding-conventions.html)
- Nombres de variables y funciones en **camelCase**
- Nombres de clases en **PascalCase**
- Nombres de constantes en **SCREAMING_SNAKE_CASE**
- Documentar clases y funciones públicas con KDoc

### Java (Legacy)

- No agregar código Java nuevo
- Si se modifica Java existente, considerar migrar a Kotlin
- Mantener estilo consistente con el código existente

### Compose

- Composables con nombre en **PascalCase**
- State hoisting: pasar estado y callbacks como parámetros
- Screens en `shared/src/commonMain/.../ui/screens/`
- Componentes reutilizables en `shared/src/commonMain/.../ui/components/`

---

## Testing

```bash
# Unit tests (shared module)
./gradlew :shared:test

# Unit tests (app module)
./gradlew :app:testNonRootDebugUnitTest

# Android instrumented tests (requiere emulador/device)
./gradlew :app:connectedNonRootDebugAndroidTest
```

### Dónde poner tests

| Tipo | Ubicación |
|------|-----------|
| Lógica de negocio | `shared/src/commonTest/` |
| ViewModels | `shared/src/commonTest/` |
| Android-specific | `app/src/test/` |
| UI (instrumented) | `app/src/androidTest/` |

---

## Estructura de Archivos

Al agregar una nueva feature:

1. **Modelo** → `shared/src/commonMain/.../model/`
2. **Lógica** → `shared/src/commonMain/.../network/` o nueva carpeta
3. **UI Screen** → `shared/src/commonMain/.../ui/screens/`
4. **UI Component** → `shared/src/commonMain/.../ui/components/`
5. **Platform callback** → Agregar a `PlatformActions` interface
6. **Android implementation** → `app/src/main/java/.../ui/premium/`
7. **Tests** → `shared/src/commonTest/`

---

## Preguntas Frecuentes

### ¿Por qué shared/ usa `java.net.*`?

Porque actualmente ambos targets (Android y Desktop) corren en JVM. Es deuda técnica conocida — debería usar Ktor o abstracciones propias para ser truly multiplatform.

### ¿Puedo agregar una Activity Java nueva?

No. Todas las nuevas pantallas deben ser Compose screens en `shared/` o Compose activities en `app/`.

### ¿Qué es `desktopApp/`?

Un gestor de servidor Immich que se ejecuta en Windows. Está excluido del build en Phase 1 y podría moverse a un repo separado en el futuro.
