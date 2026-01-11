# Repository Guidelines

## Project Structure & Module Organization
- `app/src/main/java/com/example/hw1/`: Kotlin source (activities, game logic, data, services).
- `app/src/main/res/`: UI layouts, drawables, strings, themes, and other Android resources.
- `app/src/test/`: JVM unit tests (JUnit).
- `app/src/androidTest/`: Instrumented tests (AndroidX + Espresso).
- Root Gradle files (`build.gradle.kts`, `settings.gradle.kts`, `gradle/`): build configuration and versions.

## Build, Test, and Development Commands
- `./gradlew assembleDebug`: Build a debug APK for local runs.
- `./gradlew test`: Run JVM unit tests in `app/src/test`.
- `./gradlew connectedAndroidTest`: Run instrumented tests on a device/emulator.
- `./gradlew lint`: Run Android lint checks.
Tip: Android Studio can run the `app` configuration directly for local development.

## Coding Style & Naming Conventions
- Kotlin uses 4-space indentation and standard Android/Kotlin formatting (use Android Studio formatter).
- Package naming follows reverse-DNS: `com.example.hw1`.
- Classes and files use PascalCase (e.g., `MainActivity.kt`), methods and variables use camelCase.
- Resources use snake_case (e.g., `activity_main.xml`, `ic_launcher_round.webp`).
- Room is used for persistence; KSP generates code from annotated entities/DAOs.

## Testing Guidelines
- Unit tests: JUnit in `app/src/test`, naming `*Test.kt` (see `ExampleUnitTest.kt`).
- Instrumented tests: AndroidX JUnit/Espresso in `app/src/androidTest`, naming `*Test.kt`.
- No explicit coverage target is defined; add tests for new logic or bug fixes.

## Commit & Pull Request Guidelines
- Git history only contains an initial commit; no established message convention yet.
- Prefer short, imperative commit summaries (e.g., "Add score persistence").
- PRs should include: a clear description, linked issues (if any), and screenshots for UI changes.

## Configuration & Local Setup Notes
- `local.properties` is machine-specific (Android SDK path) and should not be edited by others.
- Keep generated outputs in `build/` and `app/build/` out of version control.
