KeyStoreViewer
--------------

Quickly view MD5, SHA1, SHA256, and public key information for app signatures for filing and more.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.seiko.keystoreviewer/)

This is a Kotlin Multiplatform project targeting Android, Desktop.

* `/shared` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.
* `/androidApp` is the Android application that depends on `/shared`.
* `/desktopApp` is the Compose Desktop application that depends on `/shared`.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

Releasing
---------

Release flow: bump `versionCode`/`versionName` in `androidApp/build.gradle.kts` (and
`packageVersion` in `desktopApp/build.gradle.kts`), then push a matching `vX.Y.Z` tag.
The `Release` workflow builds the signed APK/AAB and attaches the APK to a GitHub Release.

Required repository secrets:

| Secret | Description |
| --- | --- |
| `KEYSTORE_B64` | Base64 of `keystore-viewer-release.keystore` (macOS: `base64 -i file`, Linux: `base64 -w0 file`) |
| `KEYSTORE_PASSWORD` | Keystore `storePassword` |
| `KEYSTORE_KEY_PASSWORD` | Keystore `keyPassword` |
| `KEYSTORE_ALIAS` | Optional, defaults to `KeyStoreViewer` |
| `PLAY_SERVICE_ACCOUNT_JSON_B64` | Optional, base64 of a Google Play service-account JSON; enables automatic upload of the AAB to the internal track |

Optional repository variable: `PLAY_TRACK` (defaults to `internal`).

- **Google Play**: fully automated when `PLAY_SERVICE_ACCOUNT_JSON_B64` is set — the AAB is
  uploaded via fastlane `supply` (`fastlane/play_upload` lane).
- **F-Droid**: there is no developer upload API — F-Droid builds from source on their own
  infrastructure. Tagged releases are picked up by F-Droid's update check (tag based);
  build recipe changes require a merge request against their `fdroiddata` metadata.

Signing materials live outside this repository. CI resolves them via secrets; locally,
point `signing.dir` in `local.properties` to the directory containing
`release-signing.properties` and the keystore.
