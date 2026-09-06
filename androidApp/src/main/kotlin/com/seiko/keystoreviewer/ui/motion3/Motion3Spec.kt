package com.seiko.keystoreviewer.ui.motion3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tunables of the activity-style motion transition.
 *
 * @param durationMillis duration of the non-predictive push/pop transitions.
 * @param maxScale how small the closing surface becomes at full gesture progress.
 * @param darkScrimAlpha scrim alpha used on dark surfaces.
 * @param lightScrimAlpha scrim alpha used on light surfaces; also drives the
 *   predictive-back scrim through the back handler.
 * @param enteringStartOffset how far the entering surface sits off-screen before
 *   sliding in.
 * @param margin gap kept between the closing surface and the screen edge.
 * @param cornerRadius applied to surfaces while the transition clips them.
 * @param gestureEasing maps the raw gesture progress reported by the system onto
 *   the visual progress.
 * @param followTouchVertically when `true` the closing surface also follows the
 *   finger vertically; turn it off to keep the surface vertically centred and let
 *   the gesture drive only the horizontal shift and scale.
 * @param maxYShiftFraction fraction of the surface height available for the
 *   vertical shift (before the margin is subtracted).
 * @param verticalEasing maps the finger's travelled distance onto the vertical
 *   shift limit with a decelerating curve.
 * @param settleStiffness stiffness of the spring that finishes the gesture once
 *   the finger lifts; a spring (rather than a fixed tween) is what lets the
 *   settle animation inherit the gesture velocity.
 * @param settleDampingRatio damping ratio of the settle spring.
 */
data class Motion3Spec(
  val durationMillis: Int = DEFAULT_DURATION_MILLIS,
  val maxScale: Float = DEFAULT_MAX_SCALE,
  val darkScrimAlpha: Float = DEFAULT_DARK_SCRIM_ALPHA,
  val lightScrimAlpha: Float = DEFAULT_LIGHT_SCRIM_ALPHA,
  val enteringStartOffset: Dp = DEFAULT_ENTERING_START_OFFSET,
  val margin: Dp = DEFAULT_MARGIN,
  val cornerRadius: Dp = DEFAULT_CORNER_RADIUS,
  val gestureEasing: Easing = DEFAULT_GESTURE_EASING,
  val followTouchVertically: Boolean = DEFAULT_FOLLOW_TOUCH_VERTICALLY,
  val maxYShiftFraction: Float = DEFAULT_MAX_Y_SHIFT_FRACTION,
  val verticalEasing: Easing = DEFAULT_VERTICAL_EASING,
  val settleStiffness: Float = DEFAULT_SETTLE_STIFFNESS,
  val settleDampingRatio: Float = DEFAULT_SETTLE_DAMPING_RATIO,
) {
  companion object {
    const val DEFAULT_DURATION_MILLIS: Int = 300
    const val DEFAULT_MAX_SCALE: Float = 0.9f
    const val DEFAULT_DARK_SCRIM_ALPHA: Float = 0.8f
    const val DEFAULT_LIGHT_SCRIM_ALPHA: Float = 0.2f
    val DEFAULT_ENTERING_START_OFFSET: Dp = 96.dp
    val DEFAULT_MARGIN: Dp = 8.dp
    val DEFAULT_CORNER_RADIUS: Dp = 24.dp
    val DEFAULT_GESTURE_EASING: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
    const val DEFAULT_FOLLOW_TOUCH_VERTICALLY: Boolean = true
    const val DEFAULT_MAX_Y_SHIFT_FRACTION: Float = 1f / 20f
    val DEFAULT_VERTICAL_EASING: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    const val DEFAULT_SETTLE_STIFFNESS: Float = Spring.StiffnessMediumLow
    const val DEFAULT_SETTLE_DAMPING_RATIO: Float = Spring.DampingRatioNoBouncy
  }
}
