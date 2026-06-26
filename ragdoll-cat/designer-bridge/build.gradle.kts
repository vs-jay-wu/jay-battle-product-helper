plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

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
