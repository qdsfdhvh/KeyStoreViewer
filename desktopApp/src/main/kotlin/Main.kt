import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.model.SignSource
import org.jetbrains.skia.Image
import ui.screen.DropUploadContent
import ui.screen.SignatureDetailScreen
import ui.theme.AppTheme

// Window/dock icon: PNG generated from the same 108dp vector master as the
// Android launcher icons (see desktop distribution iconFile wiring).
private fun loadWindowIcon(): Painter {
  val stream = Thread.currentThread().contextClassLoader
    .getResourceAsStream("icons/keystoreviewer-256.png")
    ?: return object : Painter() {
      override val intrinsicSize = Size.Zero

      override fun DrawScope.onDraw() {}
    }
  return stream.use { bytes ->
    BitmapPainter(Image.makeFromEncoded(bytes.readBytes()).toComposeImageBitmap())
  }
}

fun main() = application {
  val windowIcon = remember { loadWindowIcon() }
  Window(
    onCloseRequest = ::exitApplication,
    title = "KeyStoreViewer",
    icon = windowIcon,
  ) {
    AppTheme {
      val stack = remember { mutableStateListOf<DesktopPage>(DesktopPage.Upload) }

      when (val page = stack.last()) {
        DesktopPage.Upload -> DropUploadContent(
          onNavigateToDetail = { path ->
            stack.add(DesktopPage.SignatureDetail(SignSource.Apk(path)))
          },
        )

        is DesktopPage.SignatureDetail -> SignatureDetailScreen(
          signSource = page.signSource,
          onBack = {
            stack.removeAt(stack.lastIndex)
          },
        )
      }
    }
  }
}

sealed interface DesktopPage {
  data object Upload : DesktopPage

  data class SignatureDetail(val signSource: SignSource) : DesktopPage
}
