import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.signing
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    signing
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    configureMultiplatform(this)
}

android {
    configureAndroidLibrary(this)
}
