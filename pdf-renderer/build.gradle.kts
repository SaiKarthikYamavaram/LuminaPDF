import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "PdfRenderer"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    androidLibrary {
       namespace = "com.lumina.pdf.renderer"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":pdf-core"))
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(libs.pdfbox)
        }
        androidMain.dependencies {
            // PdfRenderer only rasterizes; its text APIs need API 35. This provides search,
            // text extraction and the outline back to minSdk.
            implementation(libs.pdfbox.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
