import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import data.model.SignSource
import ui.screen.DropUploadContent
import ui.screen.SignatureDetailScreen
import ui.theme.AppTheme

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "KeyStoreViewer",
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
