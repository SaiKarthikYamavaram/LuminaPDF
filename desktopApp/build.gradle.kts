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
            packageVersion = "1.0.1"

            modules("java.sql", "java.naming", "java.instrument", "jdk.unsupported", "jdk.security.auth")

            windows {
                shortcut = true
                menu = true
            }

            buildTypes.release.proguard {
                isEnabled.set(false)
            }
        }
    }
}