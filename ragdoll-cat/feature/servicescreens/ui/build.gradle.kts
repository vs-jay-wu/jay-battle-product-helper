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

// Keep Compose source info so the Designer Shell can map a tapped composable to
// its source file:line (the composeCompiler{} DSL is ignored for KMP+Android).
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-P")
        freeCompilerArgs.add("plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=true")
    }
}

android {
    namespace = "com.viewsonic.classswift.feature.servicescreens.ui"
    compileSdk = 35
    defaultConfig {
        minSdk = 28
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.viewsonic.classswift.feature.servicescreens.ui.generated.resources"
}
