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
    plugins {
        // Lets Gradle auto-provision JDK/JBR toolchains (matches the host repo).
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // Reuse the host app's version catalog rather than duplicating versions, so the
    // bridge always builds with the same Kotlin/Compose/AGP versions as its consumer.
    versionCatalogs {
        create("libs") {
            from(files("../ragdoll-cat/gradle/libs.versions.toml"))
        }
    }
}

// Standalone, reusable Compose Designer Shell bridge. Consumed by app repos via
// `includeBuild("../designer-compose-bridge")` (the Gradle equivalent of a path
// dependency) — see ragdoll-cat/settings.gradle.kts.
rootProject.name = "designer-compose-bridge"
include(":designer-node")
include(":designer-bridge")
