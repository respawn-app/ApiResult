enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    // TODO: https://github.com/Kotlin/kotlinx-atomicfu/issues/56
    resolutionStrategy {
        eachPlugin {
            val module = when (requested.id.id) {
                "kotlinx-atomicfu" -> "org.jetbrains.kotlinx:atomicfu-gradle-plugin:${requested.version}"
                else -> null
            }
            if (module != null) {
                useModule(module)
            }
        }
    }
}

dependencyResolutionManagement {
    // kmm plugin adds "ivy" repo as part of the apply block
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ApiResult"

include(":core", ":app")
