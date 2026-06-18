import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.navigation.safeargs.kotlin )
    alias(libs.plugins.roborazzi)
}

val keystorePropertiesFile = file("../keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

/**
 *  ClassSwift Service version follows the format:
 *  MAJOR.MINOR.HOTFIX.INTERNAL
 *
 *  Each component is defined as:
 * 	-    MAJOR: 3 digits - significant changes or breaking features.
 * 	-    MINOR: 2 digits - new features or enhancements that are backward compatible.
 * 	-    HOTFIX: 1 digit  - bug fixes or small patches.
 * 	-    INTERNAL: 2 digits — internal builds for QA, staging, or development.
 *
 *  Encoding: versionCode = MAJOR * 100_000 + MINOR * 1_000 + HOTFIX * 100 + INTERNAL
 *  Example: 1.0.0.0 -> 100000.
 *
 *  Version codes are defined in /version.properties.
 */
val versionPropertiesFile = file("../version.properties")
val versionProperties = Properties()
versionProperties.load(FileInputStream(versionPropertiesFile))

val versionCode = versionProperties["versionCode"].toString().toInt()

android {
    namespace = "com.viewsonic.classswift"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.viewsonic.classswift.service"
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "PLACEHOLDER_BASE_URL", "\"https://s3.amazonaws.com\"")
        buildConfigField("String", "USER_INFO_URL", "\"https://go.classswift.viewsonic.io\"")
        buildConfigField("String", "DOCUMENT_URL", "\"https://docs.viewsonic.io\"")
        buildConfigField("String", "HELP_URL", "\"https://www.classswift.viewsonic.io\"")
        buildConfigField("String", "SIGN_IN_OAUTH_CLIENT_ID", "\"${keystoreProperties["signInOAuthClientID"]}\"")
    }

    signingConfigs {
        create("viewsonic") {
            storeFile = file("${rootDir}/MVBA_PlatForm.jks")
            storePassword = "viewsonic"
            keyAlias = "platform"
            keyPassword = "viewsonic"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
        }
    }

    flavorDimensions += listOf("environment")
    productFlavors {
        // [Dimension] environment
        create("stag") {
            dimension = "environment"
            applicationIdSuffix = ".stag"
            versionNameSuffix = "-stag"
            resValue("string", "common_app_name", "ClassSwift Service Staging")
            // HTTP URL
            buildConfigField("String", "BASE_URL", "\"https://api-swift.aps1.classswift-stg.com\"")
            buildConfigField("String", "ACCOUNT_URL", "\"https://stage.cloud.viewsonic.com\"")
            buildConfigField("String", "LOGIN_REDIRECT_URL", "\"https://app-link.aps1.classswift-stg.com/account/signin\"")
            buildConfigField("String", "LOGOUT_REDIRECT_URL", "\"https://app-link.aps1.classswift-stg.com/account/signout\"")
            buildConfigField("String", "CLASS_SWIFT_HUB_URL","\"https://admin-swift.aps1.classswift-stg.com\"")
            buildConfigField("String", "LESSON_URL", "\"https://learn-swift.aps1.classswift-stg.com\"")
            buildConfigField("String", "ARTICLE_URL", "\"https://classswift.aps1.classswift-stg.com\"")
            buildConfigField("String", "OTA_UPDATE_URL", "\"https://android-ota.aps1.classswift-stg.com\"")
            buildConfigField("String", "QR_CODE_REDIRECT_URL", "\"https://admin-swift.aps1.classswift-stg.com/oauth/callback\"")
            buildConfigField("String", "MAINTENANCE_ANNOUNCEMENT_URL", "\"https://classswift-maintenance-announcements.aps1.classswift-stg.com\"")
            // Amplitude
            buildConfigField("String", "AMPLITUDE_API_KEY", "\"${keystoreProperties["amplitudeStagingApiKey"]}\"")
            // Guest Mode Login
            buildConfigField("String", "GUEST_MODE_LOGIN_API_KEY", "\"${keystoreProperties["guestModeStagingApiKey"]}\"")

            //Spinner
            buildConfigField("String", "SPINNER_URL", "\"https://swift-tools.aps1.classswift-stg.com/spinner\"")
        }
        create("rc") {
            dimension = "environment"
            applicationIdSuffix = ".rc"
            versionNameSuffix = "-rc"
            resValue("string", "common_app_name", "ClassSwift Service RC")
            // HTTP URL
            buildConfigField("String", "BASE_URL", "\"https://api-swift-rc.us.classswift.com\"")
            buildConfigField("String", "ACCOUNT_URL", "\"https://cloud.viewsonic.com\"")
            buildConfigField("String", "LOGIN_REDIRECT_URL", "\"https://app-link-rc.us.classswift.com/account/signin\"")
            buildConfigField("String", "LOGOUT_REDIRECT_URL", "\"https://app-link-rc.us.classswift.com/account/signout\"")
            buildConfigField("String", "CLASS_SWIFT_HUB_URL","\"https://admin-swift-rc.us.classswift.com\"")
            buildConfigField("String", "LESSON_URL", "\"https://learn-swift-rc.us.classswift.com\"")
            buildConfigField("String", "ARTICLE_URL", "\"https://classswift-rc.us.classswift.com\"")
            buildConfigField("String", "OTA_UPDATE_URL", "\"https://android-ota-rc.us.classswift.com\"")
            buildConfigField("String", "QR_CODE_REDIRECT_URL", "\"https://admin-swift-rc.us.classswift.com/oauth/callback\"")
            buildConfigField("String", "MAINTENANCE_ANNOUNCEMENT_URL", "\"https://classswift-maintenance-announcements-rc.us.classswift.com\"")
            // Amplitude
            buildConfigField("String", "AMPLITUDE_API_KEY", "\"${keystoreProperties["amplitudeRcApiKey"]}\"")
            // Guest Mode Login
            buildConfigField("String", "GUEST_MODE_LOGIN_API_KEY", "\"${keystoreProperties["guestModeRcApiKey"]}\"")

            //Spinner
            buildConfigField("String", "SPINNER_URL", "\"https://swift-tools-rc.us.classswift.com/spinner\"")
        }
        create("prod") {
            dimension = "environment"
            // HTTP URL
            buildConfigField("String", "BASE_URL", "\"https://api-swift.us.classswift.com\"")
            buildConfigField("String", "ACCOUNT_URL", "\"https://cloud.viewsonic.com\"")
            buildConfigField("String", "LOGIN_REDIRECT_URL", "\"https://app-link.classswift.com/account/signin\"")
            buildConfigField("String", "LOGOUT_REDIRECT_URL", "\"https://app-link.classswift.com/account/signout\"")
            buildConfigField("String", "CLASS_SWIFT_HUB_URL","\"https://classswift.manager.viewsonic.io\"")
            buildConfigField("String", "LESSON_URL", "\"https://classswift.student.viewsonic.io\"")
            buildConfigField("String", "ARTICLE_URL", "\"https://www.classswift.viewsonic.io\"")
            buildConfigField("String", "OTA_UPDATE_URL", "\"https://android-ota.viewsonic.io\"")
            buildConfigField("String", "QR_CODE_REDIRECT_URL", "\"https://classswift.manager.viewsonic.io/oauth/callback\"")
            buildConfigField("String", "MAINTENANCE_ANNOUNCEMENT_URL", "\"https://announcements.viewsonic.io\"")
            // Amplitude
            buildConfigField("String", "AMPLITUDE_API_KEY", "\"${keystoreProperties["amplitudeProductionApiKey"]}\"")
            // Guest Mode Login
            buildConfigField("String", "GUEST_MODE_LOGIN_API_KEY", "\"${keystoreProperties["guestModeProductionApiKey"]}\"")

            //Spinner
            buildConfigField("String", "SPINNER_URL", "\"https://classswift.tools.viewsonic.io/spinner\"")

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }
    testOptions {
        // Required by Robolectric so unit tests can access R.* resources and
        // inflate XML layouts — without this Roborazzi snapshot tests fail with
        // resource-not-found errors.
        unitTests.isIncludeAndroidResources = true
    }
}

fun convertVersionCodeToVersionName(versionCode: Int): String {
    println("[convertVersionCodeToVersionName]: versionCode = $versionCode")
    val major    = versionCode / 100_000
    val minor    = (versionCode / 1_000) % 100
    val hotfix   = (versionCode / 100) % 10
    val internal = versionCode % 100
    return "%d.%d.%d.%d".format(major, minor, hotfix, internal)
}

androidComponents {
    onVariants { variant ->
        val productEnvironment = variant.productFlavors.find { it.first == "environment" }?.second ?: ""
        val output = variant.outputs.firstOrNull() ?: return@onVariants
        val buildType = variant.buildType ?: return@onVariants
        println("=====================================")
        println("buildType = $buildType")
        println("applicationId = ${variant.applicationId.get()}")
        println("productEnvironment = $productEnvironment")

        println("Applied ViewSonic signing config")
        variant.signingConfig.setConfig(android.signingConfigs.getByName("viewsonic"))

        // App name override (env-keyed) lives inside `productFlavors.resValue(...)` per env.
        // prod uses the default `common_app_name` resource ("ClassSwift Service").

        println("Applied Version Code $versionCode")
        output.versionCode.set(versionCode)
        output.versionName.set(convertVersionCodeToVersionName(versionCode))
        println("versionName = ${output.versionName.get()}")
        println("versionCode = ${output.versionCode.get()}")

        /**
         *  Use Customize APK File Name
         */
        val underscoreVersionName = output.versionName.get()!!.replace(".", "_")
        val apkFileName = "ClassSwift_Service_${underscoreVersionName}_${productEnvironment.capitalized()}_${buildType.capitalized()}.apk"
        println("apkFileName = $apkFileName")
        (output as com.android.build.api.variant.impl.VariantOutputImpl).outputFileName = apkFileName

        /**
         *  Truncate version name when building prod App
         */
        if (productEnvironment == "prod") {
            output.versionName.get()?.let { currentVersionName ->
                val shortVersionName = currentVersionName.split(".").take(3).joinToString(separator = ".")
                println("✅ Overriding versionName for prod: $currentVersionName to $shortVersionName")
                variant.outputs.forEach { output ->
                    output.versionName.set(shortVersionName) // Correct way to modify versionName
                }
            }
        }
    }
}

fun getVersionNameFromGit(): String {
    val commitSha = "git rev-parse --short HEAD".runCommand().trim()
    return commitSha
}

fun String.runCommand(): String {
    val process = ProcessBuilder(*this.split(" ").toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .start()
    return process.inputStream.bufferedReader().readText()
}

tasks.register("assembleProductionApks") {
    doFirst {
        println("[Task] assembleProductionApks")
    }
    group = "build"
    description = "Assembles all production related APKs"

    val defaultOutputDir = System.getProperty("user.home") + "/Downloads"
    var outputDir = project.findProperty("outputDir")?.toString() ?: defaultOutputDir
    if (outputDir.startsWith("~")) {
        outputDir = outputDir.replaceFirst("~", System.getProperty("user.home"))
    }

    tasks.filter {
        val isDebugRelease = it.name.endsWith("Debug") || it.name.endsWith("Release")
        val isProduction = it.name.contains("Prod")
        it.name.startsWith("assemble") && isProduction && isDebugRelease
    }.forEach { task ->
        dependsOn(task)
    }

    doLast {
        val apkFiles = fileTree("build/outputs/apk") {
            include("prod/**/*.apk")
        }.files

        if (apkFiles.isEmpty()) {
            println("No APKs found to copy.")
            return@doLast
        }

        apkFiles.forEach { apk ->
            copy {
                println("Copy file: ${apk.absolutePath} into $outputDir")
                from(apk)
                into(outputDir)
            }
        }
    }
}

tasks.register("assembleReleaseCandidateApks") {
    doFirst {
        println("[Task] assembleReleaseCandidateApks")
    }
    group = "build"
    description = "Assembles all release candidate related APKs"

    val defaultOutputDir = System.getProperty("user.home") + "/Downloads"
    var outputDir = project.findProperty("outputDir")?.toString() ?: defaultOutputDir
    if (outputDir.startsWith("~")) {
        outputDir = outputDir.replaceFirst("~", System.getProperty("user.home"))
    }

    tasks.filter {
        val isDebugRelease = it.name.endsWith("Debug") || it.name.endsWith("Release")
        val isReleaseCandidate = it.name.contains("Rc")
        it.name.startsWith("assemble") && isReleaseCandidate && isDebugRelease
    }.forEach { task ->
        dependsOn(task)
    }

    doLast {
        val apkFiles = fileTree("build/outputs/apk") {
            include("rc/**/*.apk")
        }.files

        if (apkFiles.isEmpty()) {
            println("No APKs found to copy.")
            return@doLast
        }

        apkFiles.forEach { apk ->
            copy {
                println("Copy file: ${apk.absolutePath} into $outputDir")
                from(apk)
                into(outputDir)
            }
        }
    }
}

tasks.register("assembleStageApks") {
    doFirst {
        println("[Task] assembleStageApks")
    }
    group = "build"
    description = "Assembles all stage related APKs"

    val defaultOutputDir = System.getProperty("user.home") + "/Downloads"
    var outputDir = project.findProperty("outputDir")?.toString() ?: defaultOutputDir
    if (outputDir.startsWith("~")) {
        outputDir = outputDir.replaceFirst("~", System.getProperty("user.home"))
    }

    tasks.filter {
        val isDebugRelease = it.name.endsWith("Debug") || it.name.endsWith("Release")
        val isStage = it.name.contains("Stag")
        it.name.startsWith("assemble") && isStage && isDebugRelease
    }.forEach { task ->
        dependsOn(task)
    }

    doLast {
        val apkFiles = fileTree("build/outputs/apk") {
            include("stag/**/*.apk")
        }.files

        if (apkFiles.isEmpty()) {
            println("No APKs found to copy.")
            return@doLast
        }

        apkFiles.forEach { apk ->
            copy {
                println("Copy file: ${apk.absolutePath} into $outputDir")
                from(apk)
                into(outputDir)
            }
        }
    }
}

tasks.register("buildAllApkAndBundleFiles") {
    doFirst {
        println("[Task] buildAllApkAndBundleFiles")
    }
    group = "build"
    description = "Assembles all files for release used."

    tasks.filter {
        it.name == "assembleProductionApks" || it.name == "assembleReleaseCandidateApks" || it.name == "assembleStageApks"
    }.forEach { task ->
        dependsOn(task)
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.versionProtobufKotlinLite.get()}"
    }

    // Generates the java Protobuf-lite code for the Protobufs in this project. See
    // https://github.com/google/protobuf-gradle-plugin#customizing-protobuf-compilation
    // for more information.
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    // VSApi (formerly under app/src/aosp/build.gradle.kts; runtime gating via VSApiGateway)
    compileOnly(files("libs/VSApi-release.aar"))
    implementation(files("libs/VSApiCompat-release.aar"))

    // AndroidX / Google
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.browser)
    implementation(libs.protobuf.java.lite)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.paging)

    // Third Party Library
    implementation(libs.koin.android)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.adapters)
    implementation(libs.lottie)
    implementation(libs.timber)
    implementation(libs.zxing)
    implementation(libs.socketio.client) {
        exclude(group = "org.json", module = "json")
    }
    // Coil v2.6.0 (https://github.com/coil-kt/coil/blob/c490113fec34c5414a0a02d371c585b94b6b5b94/README.md)
    implementation(libs.coil)
    implementation(libs.amplitude)

    // KSP
    ksp(libs.moshi.kotlin.codegen)
    ksp(libs.room.compiler)

    // Unit Test
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.junit)     // AndroidJUnit4 runner for Robolectric tests
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)

    // Screenshot Test (Roborazzi runs on JVM via Robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)

    // Ui Test
    androidTestImplementation(libs.androidx.espresso.core)
}