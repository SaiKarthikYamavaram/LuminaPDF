import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// ── Version ───────────────────────────────────────────────────────────────────

val appVersionCode = 1
val appVersionName = "1.0"
val appId          = "com.lumina.pdf"
val jvmVersion     = JvmTarget.JVM_11
val javaVersion    = JavaVersion.VERSION_11

// ── Plugins ───────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// ── Kotlin ────────────────────────────────────────────────────────────────────

kotlin {
    compilerOptions {
        jvmTarget = jvmVersion
    }
}

// ── Signing ───────────────────────────────────────────────────────────────────

val keystorePropertiesFile = project.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

fun Properties.getOrEnv(key: String): String? =
    getProperty(key) ?: System.getenv(key.uppercase().replace(".", "_"))

val hasSigningConfig = listOf("keyAlias", "keyPassword", "storeFile", "storePassword")
    .all { keystoreProperties.getOrEnv(it) != null }

// ── Android ───────────────────────────────────────────────────────────────────

android {
    namespace  = appId
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = appId
        minSdk        = libs.versions.android.minSdk.get().toInt()
        targetSdk     = libs.versions.android.targetSdk.get().toInt()
        versionCode   = appVersionCode
        versionName   = appVersionName
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                keyAlias      = keystoreProperties.getOrEnv("keyAlias")
                keyPassword   = keystoreProperties.getOrEnv("keyPassword")
                storeFile     = keystoreProperties.getOrEnv("storeFile")?.let(project::file)
                storePassword = keystoreProperties.getOrEnv("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningConfig) signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// ── Output naming ─────────────────────────────────────────────────────────────

val releaseOutputDir = project.layout.buildDirectory.dir("outputs/apk/release").get().asFile
val aabOutputDir     = project.layout.buildDirectory.dir("outputs/bundle/release").get().asFile

tasks.matching { it.name == "assembleRelease" }.configureEach {
    doLast {
        val src = File(releaseOutputDir, "androidApp-release.apk")
        val dst = File(releaseOutputDir, "LuminaPDF-$appVersionName.apk")
        if (src.exists()) src.copyTo(dst, overwrite = true)
    }
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    doLast {
        val src = File(aabOutputDir, "androidApp-release.aab")
        val dst = File(aabOutputDir, "LuminaPDF-$appVersionName.aab")
        if (src.exists()) src.copyTo(dst, overwrite = true)
    }
}

// ── Dependencies ──────────────────────────────────────────────────────────────

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}