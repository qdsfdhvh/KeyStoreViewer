# GMS libraries (play-services-base/basement, pulled in by ads/app-update/UMP)
# declare androidx.fragment as a Maven dependency and a few of their classes
# reference it: the fragment-based GoogleApiAvailability error dialog, the
# legacy Fragment lifecycle bridge (LifecycleCallback/zzd) and the unused
# SupportFragmentWrapper. The play flavor deps exclude the fragment artifact
# (this app is pure Compose and no code path in the resolved graph reaches
# those classes), so the missing classes are expected and harmless.
-dontwarn androidx.fragment.**

# Jetpack WindowManager (via androidx.compose.ui) references the OEM-provided
# window extensions/sidecar APIs, which exist on devices but never on an app
# compile classpath.
-dontwarn androidx.window.extensions.**
-dontwarn androidx.window.sidecar.**
