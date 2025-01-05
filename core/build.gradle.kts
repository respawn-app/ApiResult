plugins {
    id("pro.respawn.shared-library")
    alias(libs.plugins.maven.publish)
    dokkaDocumentation
    // alias(libs.plugins.atomicfu)
}

android {
    namespace = Config.namespace
}

dependencies {
    commonMainApi(libs.kotlin.coroutines.core)

    jvmTestImplementation(libs.bundles.unittest)
}
