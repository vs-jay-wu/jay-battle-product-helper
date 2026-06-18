plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(project(":core:designsystem"))
        }
    }
}

android {
    namespace = "com.viewsonic.classswift.core.ui"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
    }
}
