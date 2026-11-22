# Lettering

<img align="right" src="assets/lettering.png" alt="Lettering app icon" width="112">

Lettering is an Android keyboard for applying expressive Unicode text styles while you type. It includes more than 30 styles, configurable keyboard layouts, and style ordering controls.

## Development

The Android application is the `:base` Gradle module. Open the project in Android Studio with an Android SDK installed, or use an installed Gradle:

```sh
gradle :base:assembleDebug
gradle :base:testDebugUnitTest
gradle :base:connectedDebugAndroidTest
```

The connected test task requires an emulator or Android device. This repository does not include the Gradle wrapper scripts.

## Contributing

Report bugs or request features in [GitHub Issues](https://github.com/diepet/lettering/issues). Pull requests should summarize the change, reference an issue when relevant, and include screenshots for visible UI changes.

Localized strings are maintained in `base/src/main/res/values-*`; Play Store listing text is in `fastlane/metadata/android`.
