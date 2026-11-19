// Scales for Android: three modules. `core` is pure Kotlin (theory, session, geometry) and
// mirrors the Swift sources file for file; `ui` holds the Compose painters both screens
// share; `app` is the one application with the phone and TV activities.
pluginManagement {
    repositories {
        // Google's repository serves only Android and AndroidX artefacts; the filter stops
        // Gradle from asking it for everything else first.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Repositories live here only, so no module can quietly add its own.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Scales"

include(":core", ":ui", ":app")
