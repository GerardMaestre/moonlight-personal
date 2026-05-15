pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "moonlight-personal"

include(":app")
include(":shared")
// desktopApp removed in Phase 1 — see docs/ARCHITECTURE.md

