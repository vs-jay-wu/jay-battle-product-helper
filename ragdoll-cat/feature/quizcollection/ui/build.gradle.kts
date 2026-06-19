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
            implementation(compose.components.resources)
            implementation(project(":core:designsystem"))
            implementation(project(":core:ui"))
        }
    }
}

android {
    namespace = "com.viewsonic.classswift.feature.quizcollection.ui"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.viewsonic.classswift.feature.quizcollection.ui.generated.resources"
}
