# Google Play in-app updates

Only the `play` Android flavor links `com.google.android.play:app-update`. The
`foss` flavor provides an empty `StoreUpdatePrompt`; Desktop is unchanged.

## Behavior

- Check availability when the app resumes. Missing Play Store, an ineligible
  installation/account, or an unsuccessful background check does not block usage.
- Show a non-modal banner above the tabs when a flexible update is available.
  Only tapping **Update** opens Google's consent flow. No automatic modal competes
  with ad consent, rewarded ads, file selection or navigation.
- Fetch fresh update information for each attempt; Play's consent token is
  single-use. Canceling leaves the app usable. **Later** suppresses availability
  prompts for the current Activity session (not a persisted opt-out).
- Play downloads in the background. When downloaded, offer **Restart** and
  **Later**. Only an explicit Restart/installation Retry calls `completeUpdate`;
  there are no forced or immediate updates.
- Recheck downloaded status on foreground entry, including after recreation or
  a download that finished while the app was away. Dismissing a downloaded prompt
  defers it until the next foreground entry.
- Register the install listener while resumed and remove it on pause/disposal.
  Generation/revision guards reject obsolete callbacks. Start/install failures
  are dismissible and retryable.

## Automated checks

```sh
./gradlew spotlessCheck :androidApp:testPlayDebugUnitTest \
  :androidApp:assembleFossDebug :androidApp:assemblePlayDebug \
  :shared:desktopTest :desktopApp:compileKotlin
```

The Play unit suite tests the real controller with a fake client, including
explicit consent/restart, listener cleanup, downloaded recovery, stale callbacks,
retry with new information and failure/cancellation. It does **not** prove the
Google Play service, download or package replacement works on a device.

## Play acceptance before rollout

Use a Play test track or [internal app sharing][test]. You need:

1. A test account that owns the app through Google Play, and an installed version
   containing this integration.
2. A newer eligible version with a **higher versionCode**, matching application ID
   and signing identity. A locally sideloaded debug APK alone is not sufficient.
3. For internal app sharing, open the newer build's sharing link but do not install
   it from that Play page; return to the installed app.
4. Exercise Update → cancel, Update → accept/download, background/foreground and
   recreation during download, Later → resume after download, and Restart.
5. Verify the new version is installed, and check unavailable/offline behavior.

This integration does not change version codes, upload builds, or publish a
release. Update priority/immediate-update policy is intentionally not configured.

[guide]: https://developer.android.com/guide/playcore/in-app-updates/kotlin-java
[test]: https://developer.android.com/guide/playcore/in-app-updates/test

Implementation follows the [official Kotlin/Java guide][guide].
