package com.seiko.keystoreviewer.ui.motion3

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigationevent.NavigationEvent

/**
 * Non-predictive push: the entering surface slides in from the end while the
 * exiting one slides a third of the way out and fades quickly.
 */
fun motion3TransitionSpec(
  durationMillis: Int = Motion3Spec.DEFAULT_DURATION_MILLIS,
): ContentTransform = ContentTransform(
  targetContentEnter = slideInHorizontally(tween(durationMillis)) { it } +
    fadeIn(tween(durationMillis)),
  initialContentExit = slideOutHorizontally(tween(durationMillis)) { -it / 3 } +
    fadeOut(tween(durationMillis / 2)),
)

/** Non-predictive pop: mirror image of [motion3TransitionSpec]. */
fun motion3PopTransitionSpec(
  durationMillis: Int = Motion3Spec.DEFAULT_DURATION_MILLIS,
): ContentTransform = ContentTransform(
  targetContentEnter = slideInHorizontally(tween(durationMillis)) { -it / 3 } +
    fadeIn(tween(durationMillis)),
  initialContentExit = slideOutHorizontally(tween(durationMillis)) { it } +
    fadeOut(tween(durationMillis / 2)),
)

/**
 * Predictive pop: both surfaces hold their position because the scene decorator
 * already shows the gesture-driven transform; this only keeps them stable while
 * the commit animation plays.
 */
@Suppress("UNUSED_PARAMETER")
fun motion3PredictivePopTransitionSpec(
  @NavigationEvent.SwipeEdge swipeEdge: Int = NavigationEvent.EDGE_NONE,
  durationMillis: Int = Motion3Spec.DEFAULT_DURATION_MILLIS,
): ContentTransform = ContentTransform(
  targetContentEnter = fadeIn(
    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
    initialAlpha = 1f,
  ),
  initialContentExit = fadeOut(
    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
    targetAlpha = 1f,
  ),
)
