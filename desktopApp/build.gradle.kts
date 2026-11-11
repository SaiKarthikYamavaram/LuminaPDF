import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.lumina.pdf.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "Lumina"
            packageVersion = "1.0.2"
            fileAssociation(mimeType = "application/pdf", extension = "pdf", description = "PDF document")

            modules("java.sql", "java.naming", "java.instrument", "jdk.unsupported", "jdk.security.auth")

            macOS {
                iconFile.set(project.file("icons/lumina.icns"))
            }

            windows {
                shortcut = true
                menu = true
                iconFile.set(project.file("icons/lumina.ico"))
            }

            linux {
                iconFile.set(project.file("icons/lumina.png"))
            }

            buildTypes.release.proguard {
                isEnabled.set(false)
            }
        }
    }
}