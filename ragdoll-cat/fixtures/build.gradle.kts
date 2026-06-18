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
            implementation(project(":feature:quizcollection:ui"))
        }
    }
}

android {
    namespace = "com.viewsonic.classswift.fixtures"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
    }
}
