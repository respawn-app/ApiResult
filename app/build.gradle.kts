plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    alias(libs.plugins.compose.compiler)
}

android {
    configureAndroid()

    namespace = "${Config.namespace}.sample"
    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = Config.artifactId
        minSdk = Config.appMinSdk
        targetSdk = Config.targetSdk
        versionCode = 1
        versionName = Config.versionName
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    kotlinOptions {
        jvmTarget = Config.jvmTarget.target
    }
}

dependencies {
    api(projects.core)
    implementation(libs.androidx.core)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.activity)
    implementation(libs.compose.preview)
    implementation(libs.compose.lifecycle.runtime)
    implementation(libs.compose.lifecycle.viewmodel)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.tooling)
}
