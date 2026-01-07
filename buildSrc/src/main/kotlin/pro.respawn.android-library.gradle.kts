import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("android")
    id("com.android.library")
}

kotlin {
    explicitApi()
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        jvmTarget.set(Config.jvmTarget)
    }
}

android {
    configureAndroidLibrary(this)
}
