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

    const val majorRelease = 2
    const val minorRelease = 1
    const val patch = 0
    const val postfix = "" // include dash
    const val versionName = "$majorRelease.$minorRelease.$patch$postfix"
    const val url = "https://github.com/respawn-app/ApiResult"
    const val licenseName = "The Apache Software License, Version 2.0"
    const val licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"
    const val scmUrl = "https://github.com/respawn-app/ApiResult.git"
    const val name = "ApiResult"

    @Suppress("MaxLineLength")
    const val description = """ApiResult is a Kotlin Multiplatform declarative error handling library. Just like Arrow's Either, but without the complexity."""
    const val supportEmail = "hello@respawn.pro"
    const val vendorName = "Respawn Open Source Team"
    const val vendorId = "respawn-app"
    const val developerUrl = "https://respawn.pro"

    // kotlin

    val jvmTarget = JvmTarget.JVM_11
    val javaVersion = JavaVersion.VERSION_11
    const val compileSdk = 35
    const val targetSdk = compileSdk
    const val minSdk = 21
    const val appMinSdk = 26
    const val publishingVariant = "release"

    val optIns = listOf(
        "kotlinx.coroutines.ExperimentalCoroutinesApi",
        "kotlinx.coroutines.FlowPreview",
        "kotlin.RequiresOptIn",
        "kotlin.experimental.ExperimentalTypeInference",
        "kotlin.uuid.ExperimentalUuidApi",
        "kotlin.contracts.ExperimentalContracts",
    )
    val compilerArgs = listOf(
        "-Xbackend-threads=0", // parallel IR compilation
        "-Xexpect-actual-classes",
        "-Xwasm-use-new-exception-proposal",
        "-Xconsistent-data-class-copy-visibility",
        "-Xsuppress-warning=NOTHING_TO_INLINE",
        "-Xsuppress-warning=UNUSED_ANONYMOUS_PARAMETER",
        "-Xwasm-debugger-custom-formatters"
    )
    val jvmCompilerArgs = buildList {
        addAll(compilerArgs)
        add("-Xjvm-default=all") // enable all jvm optimizations
        add("-Xcontext-receivers")
        add("-Xstring-concat=inline")
        add("-Xlambdas=indy")
        add("-Xjdk-release=${jvmTarget.target}")
    }

    // android
    const val namespace = artifactId
    const val testRunner = "androidx.test.runner.AndroidJUnitRunner"
    const val isMinifyEnabledRelease = false
    const val isMinifyEnabledDebug = false
    const val defaultProguardFile = "proguard-android-optimize.txt"
    const val proguardFile = "proguard-rules.pro"
    const val consumerProguardFile = "consumer-rules.pro"

    val stabilityLevels = listOf("preview", "eap", "alpha", "beta", "m", "cr", "rc")
    val minStabilityLevel = stabilityLevels.indexOf("beta")

    object Detekt {

        const val configFile = "detekt.yml"
        val includedFiles = listOf("**/*.kt", "**/*.kts")
        val excludedFiles = listOf("**/resources/**", "**/build/**", "**/.idea/**")
    }
}
