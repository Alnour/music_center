import java.util.Properties

// The one application: PhoneActivity (Material 3, touch) and TvActivity (TV Material, D-pad)
// in one APK, so a single store listing serves phones, tablets, Android TV and Fire OS.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// The release number lives in version.properties so scripts can bump it without parsing
// Kotlin. Reading it through `providers` lets the configuration cache notice edits.
val androidRoot = rootProject.layout.projectDirectory
val versionFile = androidRoot.file("version.properties")
val versionContents = providers.fileContents(versionFile)
val versionText = versionContents.asText.get()
val versionProperties = Properties()
versionProperties.load(versionText.reader())

// The upload key's details live only in ~/.gradle/gradle.properties on the owner's Mac.
// Without them the release build is unsigned, which is enough for CI and for check_apk.sh.
val uploadStoreFile = providers.gradleProperty("scalesUploadStoreFile")
val uploadStorePassword = providers.gradleProperty("scalesUploadStorePassword")
val uploadKeyAlias = providers.gradleProperty("scalesUploadKeyAlias")
val uploadKeyPassword = providers.gradleProperty("scalesUploadKeyPassword")
val hasUploadKey = uploadStoreFile.isPresent

android {
    namespace = "com.alnourkhalifa.scales"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // Permanent once published.
        applicationId = "com.alnourkhalifa.scales"
        // Fire OS 7 is Android 9.
        minSdk = 28
        // AGP 9 would default this to compileSdk (37); 36 is what Play requires and keeps
        // Android 17's behaviour changes out of the first release.
        targetSdk = 36
        versionCode = versionProperties.getProperty("versionCode").toInt()
        versionName = versionProperties.getProperty("versionName")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasUploadKey) {
            create("upload") {
                storeFile = file(uploadStoreFile.get())
                storePassword = uploadStorePassword.get()
                keyAlias = uploadKeyAlias.get()
                keyPassword = uploadKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // The app has no reflection of its own, so the default optimising rules suffice
            // and there is no proguard-rules.pro.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (hasUploadKey) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = true
        // Lint also reads :ui and :core, so a problem there fails the app's lint run.
        checkDependencies = true
        // targetSdk 36 is deliberate (see defaultConfig).
        disable += "OldTargetApi"
        // The TV banner exists only at xhdpi, the size TV launchers ask for; every other
        // icon is a vector.
        disable += "IconMissingDensityFolder"
    }
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    version.set(libs.versions.ktlint)
}

dependencies {
    implementation(project(":ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.tv.material)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}
