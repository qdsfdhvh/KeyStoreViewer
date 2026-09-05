package util

actual class ContentHandler {
  actual fun shareContent(content: String) {

  }

  actual fun copyToClipboard(content: String, label: String?) {
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    val selection = java.awt.datatransfer.StringSelection(content)
    clipboard.setContents(selection, selection)
  }
}
