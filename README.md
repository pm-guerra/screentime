# Screen Time+ (personal fork)

This is a personal fork of **[Screen Time](https://gitlab.com/Atharok/ScreenTime)** by **[Atharok](https://gitlab.com/Atharok)**.
All credit for the app goes to Atharok: the design, the usage calculation, the charts and the widget are
their work. This fork only adds a long-term usage history on top of it.

If you like the app, please support the original author via [Liberapay](https://liberapay.com/Atharok/donate)
or [Ko-fi](https://ko-fi.com/atharok), and get the official version on
[F-Droid](https://f-droid.org/packages/com.atharok.screentime/).

## What this fork changes

Android deletes its detailed usage events after about 10 days, so the original app can only show the last
week. This fork keeps its own copy:

- The usage sessions of each complete day are saved in a small local database (a few MB per year).
- A background job saves them every 12 hours, even if the app is not opened.
- The **Week** view has arrows to go back to earlier weeks, and the **Day** view can go back to any saved day.
- The history of uninstalled apps is kept, with their name.

The history starts from the day this fork is installed: data that Android has already deleted cannot be
recovered. Gaps appear if the background job cannot run for more than 10 days (phone off, app force-stopped).

The app is installed as **Screen Time+** (`io.github.pmguerra.screentimeplus`), next to the original app if
it is installed.

## Build

```
./gradlew assembleDefaultRelease
```

The APK is in `app/build/outputs/apk/default/release/`. It is signed with the key in `signing/`: keep using
that key, Android only installs updates signed with the same key (installing an APK signed with another key
requires uninstalling the app, which deletes the history).

## Update from the original app

```
git fetch upstream
git merge upstream/main
```

(`upstream` being https://gitlab.com/Atharok/ScreenTime.git)

## License

Like the original app, this fork is licensed under the
[GNU General Public License version 3 or later](https://www.gnu.org/licenses/gpl-3.0).
Modified by pm-guerra in October 2026 (usage history).

---

*Original README below.*

<div align="center"><img src="assets/ic_launcher_round.png" width="192" /></div>

## <div align="center">Screen Time</div>

<div align="center"><h4>An Android app that allows you to easily track the time you spend on your device and apps.</h4></div>

<div align="center">
    <a href="https://f-droid.org/packages/com.atharok.screentime/" target="_blank"><img src="assets/get-it-on-f-droid-badge.png" alt="Get it on F-Droid" height="90"></a>
</div>

You may also download the APK directly from [GitLab](https://gitlab.com/Atharok/ScreenTime/-/releases).

[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue)](https://www.gnu.org/licenses/gpl-3.0)

## Description

Screen Time is a simple app that lets you easily track the time you spend on your device and apps.

Features:

- Displays total screen time for your device over the past week or for a chosen day.
- Comparative graph showing screen time across different days of the week.
- Comparative graph displaying screen time for each hour of the day.
- Ability to view screen time and comparative graphs for each individual app.
- Comparative list of screen time for each app, sorted from the most used to the least used during the chosen period.
- Simple and modern interface.
- Home screen widget.
- No ads.
- No data collection (internet permissions disabled).
- Open source.
- Developed in Kotlin with Jetpack Compose and Material 3.

## Screenshots

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/01.png" width="192" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/02.png" width="192" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/03.png" width="192" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/04.png" width="192" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/05.png" width="192" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/06.png" width="192" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/07.png" width="192" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/08.png" width="192" />

## Donate

If you like Screen Time, you can support me via [Liberapay](https://liberapay.com/Atharok/donate) or [Ko-fi](https://ko-fi.com/atharok).

[![Donate](assets/liberapay-badge.png)](https://liberapay.com/Atharok/donate)
[![Donate](assets/ko-fi-badge.png)](https://ko-fi.com/atharok)

## Contribution

If you want to translate the app, please submit a merge request on GitLab.
If you have any questions regarding contributions or translations, feel free to open an issue or contact me by [e-mail](mailto:atharok@duck.com).

## Checksums

To verify the authenticity and integrity of the APK, compare its signature against the certificate fingerprints shown below:

```
SHA-256: e6cf1629d462a3686aa4ba2d37e02777257fd0a3bbe5540d8c70f80e12d111cd
SHA-1: 8d66ed4b5d105f7fecce6ebd07dddc569cab29b9
MD5: ea0d7434b89110e49386132bb005bb6b
```

## Licences

The code is licensed under the [GNU General Public License version 3 or later (GPL-3.0-or-later)](https://www.gnu.org/licenses/gpl-3.0).

Dependencies:

- [Core KTX](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Activity Compose](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Compose BOM](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Compose UI](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Compose UI Graphics](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Compose Material 3](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Compose Material Icons Extended](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Lifecycle Runtime Compose](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Navigation Compose](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [DataStore Preferences](https://github.com/androidx/androidx) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Koin AndroidX Compose](https://github.com/InsertKoinIO/koin) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Vico](https://github.com/patrykandpatrick/vico) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
- [Kotlin Serialization](https://github.com/Kotlin/kotlinx.serialization) is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
