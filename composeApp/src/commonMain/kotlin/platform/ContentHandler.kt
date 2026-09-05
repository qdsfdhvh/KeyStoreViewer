package platform

import androidx.compose.runtime.staticCompositionLocalOf

expect class ContentHandler {
  fun shareContent(content: String)

  fun copyToClipboard(content: String, label: String? = null)
}

val LocalContentHandler = staticCompositionLocalOf<ContentHandler> {
  error("No ContentHandler provided")
}
