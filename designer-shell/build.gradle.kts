import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.gradle.ComposeHotRun

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.compose.hot-reload") version "1.1.1"
}

kotlin {
    jvmToolchain(17)
}

// Keep Compose source information in the bytecode so ui-tooling-data can map a
// composable back to its source file + line (used by the Compose target adapter).
composeCompiler {
    includeSourceInformation = true
}

// Develop the shell's own UI with Compose Hot Reload: `./gradlew hotRun --auto`
// edits its Compose code and hot-swaps into the running window. Pre-set the entry
// point so neither hotRun nor reload needs --mainClass each time.
tasks.withType<ComposeHotRun>().configureEach {
    mainClass.set("com.viewsonic.designershell.MainKt")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.uiTooling)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

// De-risk experiment: prove tap -> composable source location works on Compose.
tasks.register<JavaExec>("runProbe") {
    group = "application"
    description = "Run the Compose source-location de-risk probe."
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.viewsonic.designershell.probe.SourceLocationProbeKt")
}

compose.desktop {
    application {
        mainClass = "com.viewsonic.designershell.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "DesignerShell"
        }
    }
}
