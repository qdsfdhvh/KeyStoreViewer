package ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Approved design direction "A · 系统蓝" (2026-09-08): grouped iOS-like
// utility styling rendered with native platform behavior. Light uses a
// grouped #F2F2F7 background with white surfaces; dark uses black with
// #1C1C1E surfaces. Interactive blue #0062CC (light) / #69ADFF (dark),
// every foreground paired against its container for >=4.5:1 contrast.
internal val LightColors = lightColorScheme(
  primary = Color(0xFF0062CC),
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFE5EFFF),
  onPrimaryContainer = Color(0xFF003B78),
  inversePrimary = Color(0xFF69ADFF),
  secondary = Color(0xFF4C5B6E),
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFE2E8F0),
  onSecondaryContainer = Color(0xFF222C38),
  tertiary = Color(0xFF246D3A),
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFE8F4EB),
  onTertiaryContainer = Color(0xFF0E3D1F),
  background = Color(0xFFF2F2F7),
  onBackground = Color(0xFF1C1C1E),
  surface = Color(0xFFFFFFFF),
  onSurface = Color(0xFF1C1C1E),
  surfaceVariant = Color(0xFFE5E5EA),
  onSurfaceVariant = Color(0xFF64646B),
  outline = Color(0xFFB4B4BC),
  outlineVariant = Color(0xFFD9D9DF),
  scrim = Color(0xFF000000),
  inverseSurface = Color(0xFF2C2C2E),
  inverseOnSurface = Color(0xFFF5F5F7),
  error = Color(0xFFBA1A1A),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF7F7FA),
  surfaceContainer = Color(0xFFF2F2F7),
  surfaceContainerHigh = Color(0xFFECECF1),
  surfaceContainerHighest = Color(0xFFE5E5EA),
  surfaceBright = Color(0xFFFFFFFF),
  surfaceDim = Color(0xFFDBDBE0),
)

internal val DarkColors = darkColorScheme(
  primary = Color(0xFF69ADFF),
  onPrimary = Color(0xFF001D42),
  primaryContainer = Color(0xFF152D49),
  onPrimaryContainer = Color(0xFFC7E1FF),
  inversePrimary = Color(0xFF0062CC),
  secondary = Color(0xFFB0C0D4),
  onSecondary = Color(0xFF1B2A3A),
  secondaryContainer = Color(0xFF2C3A4A),
  onSecondaryContainer = Color(0xFFD5E2F0),
  tertiary = Color(0xFF81D399),
  onTertiary = Color(0xFF082A16),
  tertiaryContainer = Color(0xFF153522),
  onTertiaryContainer = Color(0xFFA5EFC0),
  background = Color(0xFF000000),
  onBackground = Color(0xFFF5F5F7),
  surface = Color(0xFF1C1C1E),
  onSurface = Color(0xFFF5F5F7),
  surfaceVariant = Color(0xFF2C2C2E),
  onSurfaceVariant = Color(0xFFAAAAB2),
  outline = Color(0xFF8E8E97),
  outlineVariant = Color(0xFF38383C),
  scrim = Color(0xFF000000),
  inverseSurface = Color(0xFFF5F5F7),
  inverseOnSurface = Color(0xFF2C2C2E),
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005),
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6),
  surfaceContainerLowest = Color(0xFF000000),
  surfaceContainerLow = Color(0xFF161618),
  surfaceContainer = Color(0xFF1C1C1E),
  surfaceContainerHigh = Color(0xFF2C2C2E),
  surfaceContainerHighest = Color(0xFF38383C),
  surfaceBright = Color(0xFF3A3A3C),
  surfaceDim = Color(0xFF141416),
)

// Platform default sans at sp sizes tuned to the approved large-title /
// grouped-list language (no bundled font assets). Headings tighten slightly,
// body text keeps the platform rhythm.
private val AppTypography = Typography(
  displayLarge = TextStyle(
    fontSize = 34.sp,
    lineHeight = 41.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.6).sp,
  ),
  displayMedium = TextStyle(
    fontSize = 28.sp,
    lineHeight = 34.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.5).sp,
  ),
  displaySmall = TextStyle(
    fontSize = 24.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.4).sp,
  ),
  headlineLarge = TextStyle(
    fontSize = 34.sp,
    lineHeight = 41.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.6).sp,
  ),
  headlineMedium = TextStyle(
    fontSize = 28.sp,
    lineHeight = 34.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.5).sp,
  ),
  headlineSmall = TextStyle(
    fontSize = 22.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.3).sp,
  ),
  titleLarge = TextStyle(
    fontSize = 20.sp,
    lineHeight = 26.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.3).sp,
  ),
  titleMedium = TextStyle(
    fontSize = 17.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.2).sp,
  ),
  titleSmall = TextStyle(
    fontSize = 15.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.1).sp,
  ),
  bodyLarge = TextStyle(
    fontSize = 17.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.2).sp,
  ),
  bodyMedium = TextStyle(
    fontSize = 15.sp,
    lineHeight = 22.sp,
    letterSpacing = (-0.1).sp,
  ),
  bodySmall = TextStyle(
    fontSize = 13.sp,
    lineHeight = 20.sp,
  ),
  labelLarge = TextStyle(
    fontSize = 15.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Medium,
  ),
  labelMedium = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium,
  ),
  labelSmall = TextStyle(
    fontSize = 11.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Medium,
  ),
)

// Grouped-list corners per the approved direction: 12dp cards/lists,
// sheets round to 28dp.
private val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(8.dp),
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun AppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkColors else LightColors,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
