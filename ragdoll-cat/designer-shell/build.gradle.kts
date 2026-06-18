import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hotreload)
}

kotlin {
    jvmToolchain(17)
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":core:designsystem"))
            implementation(project(":core:ui"))
            implementation(project(":feature:playground:ui"))
            implementation(project(":fixtures"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.viewsonic.classswift.designershell.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "DesignerShell"
        }
    }
}
