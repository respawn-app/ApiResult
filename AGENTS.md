# Repository Guidelines

## Project Structure & Module Organization

- `core/` is the Kotlin Multiplatform library. Shared code lives in `core/src/commonMain/kotlin/pro/respawn/apiresult/`.
- JVM tests live in `core/src/jvmTest/kotlin/pro/respawn/apiresult/test/`.
- `app/` is a sample Android app. Kotlin sources are under `app/src/main/kotlin/…` and resources in `app/src/main/res/`.
- `buildSrc/` contains Gradle convention plugins and shared build logic.
- `docs/` holds documentation sources; `detekt.yml` defines lint rules.

## Build, Test, and Development Commands

- `./gradlew :core:jvmTest` — runs JVM unit tests for the library.
- `./gradlew :app:assembleDebug` — builds the sample app APK.
- `./gradlew detektAll` — runs Detekt across the project.
- `./gradlew detektFormat` — applies Detekt auto-corrections.

## Testing Guidelines

- Tests use Kotest with the JUnit 5 platform.
- Place JVM tests in `core/src/jvmTest/...` and name files `*Tests.kt`.
- Add tests for new operators or error-handling behaviors; keep operator coverage high.

## Commit & Pull Request Guidelines

- Commit subjects are short, imperative, and lowercase (for example, `fix publish workflow`).
- Release/version commits use a plain version string, optionally with the PR number (for example, `2.1.0 (#14)`).
- PRs should include: a concise description, rationale for changes, and tests run. Update docs/KDoc when the public API changes.
