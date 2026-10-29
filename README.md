# Card Picker

> Turn a folder of card images into a draftable pool.

A small Android app for tabletop card games. Point it at a folder of card
images, draw a random hand of N cards, and flip through the result full screen.

## Why this exists

Many tabletop card games assume a larger pool than the physical box provides.
If you own only a few cards, you can still draft from the full set: put the card
images on your phone, point the app at the folder, and draw a random hand. The
images are whatever you already have on your phone.

## What it does

- Pick a folder once with the system folder picker; the app remembers it
- Scans that folder (and its subfolders) for image files
- Draws N cards at random, without duplicates
- Shows the hand as a grid; tap a card to open it full screen
- Swipe left or right to move through the hand; tap to hide the controls
- Remembers how many cards you usually draw

## Privacy

The app requests **no permissions at all**, including no `INTERNET` permission.
Images are read through the system document picker and decoded on the device.
Nothing is uploaded, and the app cannot make a network request even if it wanted
to.

## Build

Requirements:

- JDK 17 or newer
- Android SDK with platform 34 and build-tools 34.0.0
- Gradle 8.5+ (or use the included wrapper once generated)

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

For a release build, create a keystore once and keep it private:

```bash
keytool -genkeypair -v \
  -keystore cardpicker-release.jks \
  -alias cardpicker \
  -keyalg RSA -keysize 2048 -validity 10000
```

Add the signing config to `app/build.gradle.kts` (or pass the values through
environment variables):

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../cardpicker-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "cardpicker"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

```bash
./gradlew assembleRelease
```

Keep the keystore and its passwords out of version control. The same key is
required for every future update, and losing it means users cannot upgrade
without uninstalling first.

## Install

Download the APK from the Releases page, open it on the phone, and allow
"install unknown apps" for your browser or file manager when prompted.

The app targets Android 8.0 (API 26) and newer.

## Images and copyright

This repository contains **no card artwork**. You supply your own images.

If you use artwork from a commercial game, keep it to your own device. Do not
redistribute it, and do not bundle it into a build you share. The app itself is
only a viewer and a randomiser.

## License

MIT. See [LICENSE](LICENSE).
