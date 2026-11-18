// Pure Kotlin on the JVM: theory, session and geometry with no Android imports, so its tests
// run in seconds on any machine, Linux CI included.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // The settings and library JSON readers and writers (core/session/ScaleLibraryJson.kt).
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    // Lets the app's lint run (checkDependencies) analyse this module too.
    alias(libs.plugins.android.lint)
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    version.set(libs.versions.ktlint)
}

// The Swift generator writes expected/answers.json at the repository root; the tests read
// that live file as a resource, so no copy can go stale.
sourceSets["test"].resources.srcDir("../../expected")

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
