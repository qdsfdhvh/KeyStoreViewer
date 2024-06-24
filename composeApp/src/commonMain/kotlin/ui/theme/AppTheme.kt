package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

private val seedColor = Color(0xFFF1A53A)

@Composable
fun AppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = rememberDynamicColorScheme(
    seedColor = seedColor,
    isDark = darkTheme,
    style = PaletteStyle.Content,
  )
  MaterialTheme(
    colorScheme = colorScheme,
    content = content,
  )
}
