# Lettering

<img align="right" src="assets/lettering.png" alt="Lettering app icon" width="112">

Lettering is an Android keyboard for applying expressive Unicode text styles while you type. It includes more than 30 styles, configurable keyboard layouts, style ordering controls, and an emoji picker with offline search by name or keyword.

The app follows the language selected for Android. Its interface is available in Arabic, Catalan, Chinese (Simplified), English, Esperanto, Finnish, French, German, Italian, Japanese, Norwegian Bokmål, Portuguese (including Brazilian Portuguese), Russian, Spanish, Tamil, and Ukrainian. Emoji search uses localized Unicode CLDR annotations for 15 languages and falls back to English for Esperanto and other locales without a bundled search index. [Emoji search data and license](base/src/main/assets/emoji-search/NOTICE.md)

## Why Lettering exists

Lettering is for my daughter, Ma~, who wisely decided she didn't need a keyboard app that might keep a diary of everything she typed. So I made her one of her own: a little more expressive, a lot more personal, and built with her safety and privacy in mind. Ma~, may your words be bold, your typos be adorable, and your keyboard always mind its manners.

## First-time setup

These commands are for the Linux zsh setup used for this repository. If your SDK or JDK is installed elsewhere, change the paths below. This project uses Gradle 9.6.1 and Android Gradle Plugin 9.4.1, with JDK 17 as the minimum. Java and Kotlin sources target Java 17.

Add these lines to `~/.zshrc`, then open a new terminal or run `source ~/.zshrc`:

```sh
export ANDROID_SDK_ROOT="$HOME/android/sdk"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export JAVA_HOME="$HOME/android/jdk17-root/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
```

Check the tools are visible:

```sh
java -version       # should say 17
android info        # prints the Android CLI and SDK locations
adb version
```

Install the SDK pieces used to build this project and run the Android 16 emulator:

```sh
android sdk install platforms/android-36
android sdk install build-tools/36.0.0
android sdk install platform-tools
android sdk install emulator
android sdk install "system-images;android-36;google_apis_playstore;x86_64"
```

Create a virtual phone once, if you do not already have one. Use `android emulator list` to see existing devices; use `android emulator create --list-profiles` to see other device profiles.

```sh
android emulator create medium_phone
```

More details: [Android command-line tools](https://developer.android.com/tools) and [Android Virtual Devices](https://developer.android.com/studio/run/managing-avds).

## Run the app in two terminals

In **Terminal 1**, start the emulator and leave it running:

```sh
android emulator start medium_phone
```

If the command says the emulator is already running but no window is visible, restart it in windowed mode:

```sh
android emulator stop medium_phone
android emulator start medium_phone
```

In **Terminal 2**, build, find the emulator's device ID, then install and open Lettering:

```sh
cd "$HOME/dev/android/lettering"
./gradlew :base:assembleDebug
adb devices -l
android run --device=emulator-5554 --apks=base/build/outputs/apk/debug/base-debug.apk
```

If `adb devices -l` shows a different ID, replace `emulator-5554` with that ID. The debug APK is for local testing and is written to `base/build/outputs/apk/debug/base-debug.apk`.

On first launch, follow the app's setup: tap **Enable Keyboard**, allow Lettering in Android Settings, return to the app, tap **Select Keyboard**, and choose **Lettering**. Then tap the example text box to try typing. If the emulator hides the software keyboard because it has a hardware keyboard, run `adb shell settings put secure show_ime_with_hard_keyboard 1` in Terminal 2.

Stop the emulator when finished with `android emulator stop medium_phone`.

## Test on a real Android phone

The app requires Android 11 (API 30) or newer. On the phone, enable **Developer options** (usually by tapping **Build number** seven times in **Settings → About phone**), then enable **USB debugging** in Developer options. Connect the unlocked phone with a USB data cable and accept the **Allow USB debugging** prompt.

From the repository root, build and check that ADB can see the phone:

```sh
./gradlew :base:assembleDebug
adb devices -l
```

Copy the phone's serial from the first column of the `adb devices -l` output (its status should be `device`), then install and launch the app. Replace `PHONE_SERIAL` with that value:

```sh
android run --device=PHONE_SERIAL --apks=base/build/outputs/apk/debug/base-debug.apk
```

If the phone is missing from `adb devices -l`, unlock it, check its USB notification and choose **File transfer** if offered, confirm **USB debugging** is on, and approve any computer-authorization prompt. Try a USB data cable and another port (some cables only charge). On Linux, `lsusb` should show a new device when the phone is connected. On Ubuntu, if `adb devices -l` says `no permissions`, install the USB permission rules:

```sh
sudo apt update
sudo apt install android-sdk-platform-tools-common
sudo udevadm control --reload-rules
sudo udevadm trigger
```

Then unplug and reconnect the phone, unlock it, and accept the computer-authorization prompt. If `adb devices -l` says `unauthorized` and no prompt appears, open **Developer options → Revoke USB debugging authorizations**, turn USB debugging off and on, then reconnect the unlocked phone and accept the new prompt. Do not run ADB with `sudo`. If the phone is visible in `lsusb` but still missing in ADB, restart ADB with `adb kill-server` and `adb start-server`.

Follow **Enable Keyboard** and **Select Keyboard** in the app, then type in its example field. Use a test phrase rather than a password or other sensitive text while trying a third-party keyboard. When testing is done, switch back to your usual keyboard from Android's keyboard picker.

## Build and test

Run these commands from the repository root:

```sh
./gradlew :base:assembleDebug             # build a local debug APK
./gradlew :base:assembleRelease           # build an unsigned release APK
./gradlew :base:testDebugUnitTest         # run JVM tests
./gradlew :base:connectedDebugAndroidTest # run the device test on a running emulator
```

If the connected test command aborts before running tests, install the test APKs and start the test directly:

```sh
./gradlew :base:installDebug :base:installDebugAndroidTest
adb shell am instrument -w io.github.diepet.lettering.test/androidx.test.runner.AndroidJUnitRunner
```

### APK size

Compressing compiled code and packing the offline search data reduced the measured debug APK from 13.89 MiB to 6.01 MiB and the unsigned release APK from 2.39 MiB to 1.81 MiB. The build generates one compressed emoji sequence list shared by all languages and a compressed annotation index for each language.

In three runs per debug build on an API 36 emulator, median keyboard opening time went from 569 ms to 518 ms. Loading the index and running the first search went from 984 ms to 772 ms. These measurements are specific to the debug builds on that emulator.

## Publish to Google Play

**Publishing is not configured yet.** The project targets API 36. Before uploading, configure release signing and set the Play service-account key path in `fastlane/Appfile`. See [Play target API requirements](https://developer.android.com/google/play/requirements/target-sdk), [Play App Signing](https://developer.android.com/studio/publish/app-signing), and [Play Console release steps](https://support.google.com/googleplay/android-developer/answer/9859348).

Basic release steps:

1. Create/verify a Play Console developer account, then create an app with package name `io.github.diepet.lettering`. This fork's package name is different from the upstream app.
2. Complete the store listing and required Play Console forms, including app description, screenshots, content rating, and Data safety.
3. Keep `targetSdk` aligned with the current Play requirement and increase `versionCode` in `base/build.gradle` for each release.
4. Set up Play App Signing and a private upload key. Add release signing to Gradle using local secrets or environment variables; never commit keys or passwords.
5. Build the signed Android App Bundle (AAB): `./gradlew :base:bundleRelease`. The output will be under `base/build/outputs/bundle/release/`.
6. Upload the AAB to an internal testing track in Play Console, check the release, then promote it when ready.

The repository includes a Fastlane deploy lane, but it also needs signing and a Play service-account JSON configured before it can upload. Start with Play Console's internal testing track if you are new to publishing.

## Contributing

Report bugs or request features in [GitHub Issues](https://github.com/diepet/lettering/issues). Pull requests should summarize the change, reference an issue when relevant, and include screenshots for visible UI changes. Localized strings are in `base/src/main/res/values-*`; Play Store listing text is in `fastlane/metadata/android`.

## Acknowledgments

Special thanks to [Neea Rusch](https://github.com/nkrusch) and [Mobile First](https://github.com/MobileFirstLLC) for creating [Irregular Expressions](https://github.com/MobileFirstLLC/irregular-expressions), the original Android keyboard project that Lettering was forked from. Lettering builds on their work under the MIT License; see [LICENSE](LICENSE).
