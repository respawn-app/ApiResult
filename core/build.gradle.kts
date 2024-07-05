plugins {
    id("pro.respawn.shared-library")
}

android {
    namespace = Config.namespace
}

dependencies {
    commonMainApi(libs.kotlin.coroutines.core)
    jvmTestImplementation(libs.bundles.unittest)
}
