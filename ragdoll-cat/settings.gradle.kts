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
include(":app")

// --- Compose Multiplatform modules (dev-only desktop preview; see docs/desktop-app-architecture.md) ---
include(":core:designsystem")
include(":core:ui")
include(":fixtures")
include(":feature:quizcollection:ui")
include(":feature:servicescreens:ui")
include(":designer-node")
include(":designer-bridge")
include(":designer-shell")
