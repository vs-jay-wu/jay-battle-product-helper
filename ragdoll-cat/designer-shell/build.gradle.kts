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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.uiTooling)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(project(":core:designsystem"))
            implementation(project(":core:ui"))
            implementation(project(":feature:quizcollection:ui"))
            implementation(project(":fixtures"))
        }
    }
}

// Source info so the shell can map a tapped composable to its source file:line.
composeCompiler {
    includeSourceInformation = true
}

compose.desktop {
    application {
        // Compose target preview host (out-of-process target for the standalone
        // Designer Shell). Old in-process Main.kt stays as a file but isn't the entry.
        mainClass = "com.viewsonic.classswift.designershell.ComposeTargetHostKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "DesignerShell"
        }
    }
}

// Alternate entry point: the Flutter Designer Shell (docks a native flutter app).
// Run: ./gradlew :designer-shell:runFlutterShell -Dflutter.vm=http://127.0.0.1:PORT/TOKEN=/
tasks.register<JavaExec>("runFlutterShell") {
    group = "application"
    description = "Run the Flutter Designer Shell (docks a native flutter app)."
    dependsOn("jvmMainClasses")
    val mainComp = kotlin.targets.getByName("jvm").compilations.getByName("main")
    classpath(mainComp.output.allOutputs, mainComp.runtimeDependencyFiles)
    mainClass.set("com.viewsonic.classswift.designershell.flutter.FlutterShellKt")
    System.getProperty("flutter.vm")?.let { systemProperty("flutter.vm", it) }
}
