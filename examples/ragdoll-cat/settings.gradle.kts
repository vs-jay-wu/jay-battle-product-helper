pluginManagement {
    repositories {
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
plugins {
    // Lets Gradle auto-provision JDK/JBR toolchains (used by Compose Hot Reload's hotRunJvm).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ClassSwift"

// The Compose Designer Shell bridge lives at the repo root as a standalone, reusable
// Gradle build; pull it in source-level (Gradle's equivalent of a path dependency).
// Gradle auto-substitutes `com.viewsonic.designer:designer-node` / `:designer-bridge`
// with the included build's projects (see those modules' group/version).
includeBuild("../../bridges/compose")

include(":app")

// --- Compose Multiplatform modules (dev-only desktop preview; see docs/desktop-app-architecture.md) ---
include(":core:designsystem")
include(":core:ui")
include(":fixtures")
include(":feature:quizcollection:ui")
include(":feature:servicescreens:ui")
include(":designer-shell")
