package platform

enum class PlatformType {
    Android,
    Desktop,
    ;

    companion object
}

expect val PlatformType.Companion.current: PlatformType

expect val PlatformType.Companion.currentName: String
