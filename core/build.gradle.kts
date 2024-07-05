import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("pro.respawn.shared-library")
    alias(libs.plugins.maven.publish)
}

android {
    namespace = Config.namespace
}

dependencies {
    commonMainApi(libs.kotlin.coroutines.core)
    jvmTestImplementation(libs.bundles.unittest)
}

mavenPublishing {
    val properties = localProperties()
    val isReleaseBuild = properties["release"]?.toString().toBoolean()
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, false)
    signAllPublications()
    coordinates(Config.group, Config.artifact, Config.version(isReleaseBuild))
    pom {
        name = Config.name
        description = Config.description
        url = Config.url
        licenses {
            license {
                name = Config.licenseName
                url = Config.licenseUrl
                distribution = Config.licenseUrl
            }
        }
        developers {
            developer {
                id = Config.vendorId
                name = Config.vendorName
                url = Config.developerUrl
                email = Config.supportEmail
                organizationUrl = Config.developerUrl
            }
        }
        scm {
            url = Config.scmUrl
        }

    }
}
