package util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

@Composable
@ReadOnlyComposable
operator fun PaddingValues.plus(
  plus: PaddingValues,
) = plus(
  plus = plus,
  layoutDirection = LocalLayoutDirection.current,
)

fun PaddingValues.plus(
  plus: PaddingValues,
  layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): PaddingValues = PaddingValues(
  start = calculateStartPadding(layoutDirection) + plus.calculateStartPadding(layoutDirection),
  top = calculateTopPadding() + plus.calculateTopPadding(),
  end = calculateEndPadding(layoutDirection) + plus.calculateEndPadding(layoutDirection),
  bottom = calculateBottomPadding() + plus.calculateBottomPadding(),
)
