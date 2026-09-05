import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import ui.screen.DropUploadScreen
import ui.theme.AppTheme

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "KeyStoreViewer",
  ) {
    AppTheme {
      Navigator(DropUploadScreen) { navigator ->
        SlideTransition(navigator)
      }
    }
  }
}
