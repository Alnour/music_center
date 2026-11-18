// The Compose painters shared by the phone and TV screens (board, keyboard, notes row, chord
// tables). Compose Foundation only: phone Material 3 and TV Material stay in the app module.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.alnourkhalifa.scales.ui"
    // Compose 1.12 refuses a compileSdk below 37.
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        // Fire OS 7 is Android 9.
        minSdk = 28
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    version.set(libs.versions.ktlint)
}

dependencies {
    api(project(":core"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
}
