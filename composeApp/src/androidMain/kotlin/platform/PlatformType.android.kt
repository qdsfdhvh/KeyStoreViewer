package platform

import android.os.Build

actual val PlatformType.Companion.current: PlatformType
    get() = PlatformType.Android


actual val PlatformType.Companion.currentName: String
    get() = "Android ${Build.VERSION.SDK_INT}"
