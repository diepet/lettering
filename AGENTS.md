# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android application; Gradle includes the `:base` app module. Kotlin source lives in `base/src/main/java/io/github/diepet/lettering`, organized into `keyboard`, `settings`, and `styles` packages. Android layouts, drawable assets, keyboard definitions, and localized strings are under `base/src/main/res`. JVM tests belong in `base/src/test`; device tests belong in `base/src/androidTest`. Product artwork and screenshots are in `assets/`, while Play Store metadata and release automation are in `fastlane/`.

Emoji-search source TSVs are in `base/src/main/emoji-search`. Each row contains an emoji sequence, searchable text, and a localized description. The description must be the prefix of the searchable text, and every locale must use the same emoji sequences in the same order as `en.tsv`. Locale mappings and the English fallback are in `EmojiSearchIndex.kt`. [Data provenance and license](base/src/main/assets/emoji-search/NOTICE.md)

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper with JDK 17 and Android SDK 36. Java and Kotlin sources target Java 17:

- `./gradlew :base:assembleDebug` builds the debug APK.
- `./gradlew :base:assembleRelease` builds the unsigned release APK.
- `./gradlew :base:installDebug` builds and installs it on a connected device or emulator.
- `./gradlew :base:testDebugUnitTest` runs local JVM tests.
- `./gradlew :base:connectedDebugAndroidTest` runs instrumented tests on a connected device or emulator.

Open the project in Android Studio for emulator-based development and resource previews.

### APK packaging

`base/emoji-search.gradle` generates gzip-compressed `.idx` assets for each build variant from the source TSVs. Gradle generates a shared emoji sequence list and one annotations file per locale. Edit the TSV sources and let the build regenerate the assets. The Unicode license and notice ship with the indexes.

`base/build.gradle` enables DEX compression for standalone APKs and R8 code and resource shrinking for release. To check a size change, compare APKs of the same build type and inspect the packaged DEX and search assets. The APKs are under `base/build/outputs/apk/debug/` and `base/build/outputs/apk/release/`.

## Coding Style & Naming Conventions

Follow the existing Kotlin style: four-space indentation, PascalCase class and object names, and lowerCamelCase functions and properties. Keep packages lowercase and aligned with the source directory. Name tests descriptively and place them in the matching package under the appropriate test source set. No formatter or linter configuration is checked in; keep changes consistent with nearby code and Android resource naming.

## Testing Guidelines

Add or update tests for behavior changes. Use `*Test.kt` names and JUnit `@Test` methods; use `androidTest` for behavior requiring Android framework/device APIs. No coverage threshold is configured. Run the relevant Gradle test task before submitting changes when the local SDK or device setup permits.

## Commit & Pull Request Guidelines

History includes concise maintenance subjects and Conventional Commit examples such as `chore(deps): ...`; use a short, clear summary. Follow `.github/PULL_REQUEST_TEMPLATE.md`: explain the change, reference an issue or state “None,” include screenshots when useful, and complete the checklist. Keep generated APKs, signing keys, and `secret.properties` out of commits; these local files are ignored by Git.
