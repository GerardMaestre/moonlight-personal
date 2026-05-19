plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Moonlight Core Libraries for JVM / Desktop parity
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jmdns:jmdns:3.5.9")
    implementation("xmlpull:xmlpull:1.1.3.1")
    implementation("kxml2:kxml2:2.3.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "MoonlightPersonal"
            packageVersion = "1.0.0"
            
            // Bundle the complete JVM standard runtime to completely avoid any JVM startup errors on client machines
            includeAllModules = true

            windows {
                // Enable desktop shortcut, start menu entry, and directory choosing during install
                iconFile.set(project.layout.projectDirectory.file("src/main/resources/app_icon.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}
