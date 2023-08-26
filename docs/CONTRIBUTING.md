# Contributing

* To build the project, you will need the following in your local.properties:
    ```properties
    # only required for publishing
    sonatypeUsername=...
    sonatypePassword=...
    signing.key=...
    signing.password=...
    # always required
    sdk.dir=...
    release=false
    ```
* Make sure you these installed:
    * Android Studio latest Canary or Beta, depending on the current project's AGP (yes, we're on the edge).
    * Kotlin Multiplatform suite (run `kdoctor` to verify proper setup)
    * Detekt plugin
    * Kotest plugin
    * Compose plugin

* Before pushing, make sure the following tasks pass:
    * `gradle detektFormat`
    * `gradle assemble`
    * `gradle allTests`
