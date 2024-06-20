package platform

actual val PlatformType.Companion.current: PlatformType
  get() = PlatformType.Desktop

actual val PlatformType.Companion.currentName: String
  get() = "Java ${System.getProperty("java.version")}"
