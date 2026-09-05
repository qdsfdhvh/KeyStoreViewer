package platform

actual class ContentHandler {
  actual fun shareContent(content: String) {
    // Not supported on desktop
  }

  actual fun copyToClipboard(content: String, label: String?) {
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    val selection = java.awt.datatransfer.StringSelection(content)
    clipboard.setContents(selection, selection)
  }
}
