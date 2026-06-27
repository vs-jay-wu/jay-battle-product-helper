plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// Coordinates so consumers can declare `com.viewsonic.designer:designer-bridge` and
// Gradle auto-substitutes this project across the includeBuild boundary.
group = "com.viewsonic.designer"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.uiTooling)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(project(":designer-node")) // Modifier.designNode + DesignNodeRegistry (no app coupling)
        }
    }
}
