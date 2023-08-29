@file:Suppress(
    "MemberVisibilityCanBePrivate",
    "MissingPackageDeclaration",
    "UndocumentedPublicClass",
    "UndocumentedPublicProperty"
)

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object Config {

    const val group = "pro.respawn"
    const val artifact = "apiresult"

    const val artifactId = "$group.$artifact"

    const val majorRelease = 1
    const val minorRelease = 0
    const val patch = 1
    const val postfix = ""
    const val versionName = "$majorRelease.$minorRelease.$patch$postfix"
    const val url = "https://github.com/respawn-app/ApiResult"
    const val licenseName = "The Apache Software License, Version 2.0"
    const val licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    const val scmUrl = "https://github.com/respawn-app/ApiResult.git"
    const val description = """
ApiResult is a Kotlin Multiplatform declarative error handling framework that is performant, easy to use and
feature-rich.
    """

    // kotlin

    val optIns = listOf(
        "kotlinx.coroutines.ExperimentalCoroutinesApi",
        "kotlinx.coroutines.FlowPreview",
        "kotlin.RequiresOptIn",
        "kotlin.experimental.ExperimentalTypeInference",
        "kotlin.contracts.ExperimentalContracts"
    )
    val compilerArgs = listOf(
        "-Xbackend-threads=0", // parallel IR compilation
    )
    val jvmCompilerArgs = buildList {
        addAll(compilerArgs)
        add("-Xjvm-default=all") // enable all jvm optimizations
        add("-Xcontext-receivers")
        // add("-Xuse-k2")
        addAll(optIns.map { "-opt-in=$it" })
    }

    val jvmTarget = JvmTarget.JVM_11
    val javaVersion = JavaVersion.VERSION_11
    val kotlinVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
    const val compileSdk = 34
    const val minSdk = 21
    const val targetSdk = compileSdk
    const val appMinSdk = 26
    const val publishingVariant = "release"

    // android
    const val namespace = artifactId
    const val testRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val isMinifyEnabledRelease = false
    const val isMinifyEnabledDebug = false
    const val defaultProguardFile = "proguard-android-optimize.txt"
    const val proguardFile = "proguard-rules.pro"
    const val consumerProguardFile = "consumer-rules.pro"

    val stabilityLevels = listOf("preview", "eap", "alpha", "beta", "m", "cr", "rc")

    object Detekt {

        const val configFile = "detekt.yml"
        val includedFiles = listOf("**/*.kt", "**/*.kts")
        val excludedFiles = listOf("**/resources/**", "**/build/**", "**/.idea/**")
    }
}
