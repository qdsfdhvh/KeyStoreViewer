package ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class AppThemeTest {
  @Test
  fun lightSemanticTextPairsMeetContrastFloor() = checkScheme(LightColors)

  @Test
  fun darkSemanticTextPairsMeetContrastFloor() = checkScheme(DarkColors)

  private fun checkScheme(colors: ColorScheme) {
    val pairs = listOf(
      "primary" to (colors.primary to colors.onPrimary),
      "primary container" to (colors.primaryContainer to colors.onPrimaryContainer),
      "secondary" to (colors.secondary to colors.onSecondary),
      "secondary container" to (colors.secondaryContainer to colors.onSecondaryContainer),
      "tertiary" to (colors.tertiary to colors.onTertiary),
      "tertiary container" to (colors.tertiaryContainer to colors.onTertiaryContainer),
      "background" to (colors.background to colors.onBackground),
      "surface" to (colors.surface to colors.onSurface),
      "secondary text" to (colors.surface to colors.onSurfaceVariant),
      "surface variant" to (colors.surfaceVariant to colors.onSurfaceVariant),
      "error" to (colors.error to colors.onError),
      "error container" to (colors.errorContainer to colors.onErrorContainer),
      "blue action" to (colors.surface to colors.primary),
    )
    pairs.forEach { (name, pair) ->
      val (background, foreground) = pair
      assertTrue("$name must be opaque", background.alpha == 1f && foreground.alpha == 1f)
      val a = luminance(background)
      val b = luminance(foreground)
      val ratio = (max(a, b) + 0.05) / (min(a, b) + 0.05)
      assertTrue("$name contrast $ratio must be >= 4.5", ratio >= 4.5)
    }
  }

  private fun luminance(color: Color): Double {
    fun linear(channel: Float): Double = if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
    return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
  }
}
