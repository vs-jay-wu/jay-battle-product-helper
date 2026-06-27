plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Coordinates so consumers can declare `com.viewsonic.designer:designer-node` and
// Gradle auto-substitutes this project across the includeBuild boundary.
group = "com.viewsonic.designer"
version = "0.1.0"

// The Designer Shell's design-node primitive: Modifier.designNode + the registry the
// shell reads to hit-test taps. Dependency-light and multiplatform on purpose — both
// the (multiplatform) app/feature UI and the (desktop) :designer-bridge depend on it,
// so it must carry no app-specific or desktop-only baggage. Any repo can adopt the
// bridge by tagging its components with Modifier.designNode from this module.
kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}

android {
    namespace = "com.viewsonic.designer.node"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
    }
}
