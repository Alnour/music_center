// Plugins are declared here once, unapplied, so every module resolves the same versions.
// Declaring kotlin-jvm 2.4.21 at the root is what lifts AGP 9.5.1's built-in Kotlin from its
// bundled 2.2.10 to 2.4.21 for the Android modules too.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt)
}

// One detekt run over all three modules' sources, with the project's complexity limits
// layered on detekt's defaults.
detekt {
    buildUponDefaultConfig = true
    config.setFrom("detekt.yml")
    source.setFrom("core/src", "ui/src", "app/src")
}
