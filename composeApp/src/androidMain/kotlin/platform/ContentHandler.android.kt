package platform

import android.content.Context
import util.copyContent
import util.createShareTempFile
import util.shareFile

actual class ContentHandler(
  private val context: Context,
) {
  actual fun shareContent(content: String) {
    val tempFile = context.createShareTempFile(content)
    context.shareFile(tempFile)
  }

  actual fun copyToClipboard(content: String, label: String?) {
    context.copyContent(content, label)
  }
}
