package com.seiko.keystoreviewer.ui.motion3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/** Creates and remembers a [Motion3] driven by the current composition scope. */
@Composable
fun rememberMotion3(spec: Motion3Spec = Motion3Spec()): Motion3 {
  val scope = rememberCoroutineScope()
  return remember(scope, spec) { Motion3(scope, spec) }
}

/**
 * State machine behind the activity-style transition.
 *
 * A gesture walks through [Phase.GESTURE] while the finger is down; on lift it
 * either springs to [Phase.CANCEL] (surface returns) or [Phase.COMPLETE]
 * (surface commits and the navigation pops). Both settle springs start with the
 * velocity the finger had at release so the motion never restarts from zero.
 */
@Stable
class Motion3 internal constructor(
  private val scope: CoroutineScope,
  val spec: Motion3Spec,
) {
  internal var phase by mutableStateOf(Phase.IDLE)
    private set
  internal var closingKey by mutableStateOf<Any?>(null)
    private set
  internal var enteringKey by mutableStateOf<Any?>(null)
    private set
  internal var lastProgress: Float = 0f
    private set
  private var committedStartProgress: Float = 0f
  private var swipeEdge: Int = NavigationEvent.EDGE_LEFT
  private var gestureProgressProvider: () -> Float = { lastProgress }
  private var maxScrimAlpha: Float = spec.lightScrimAlpha

  // Bounded so a fast flick cannot overshoot past the end of the transition and
  // spend frames springing back to a position the transform would clamp anyway.
  // Hitting the bound ends the animation, which is exactly the desired
  // "flick finishes sooner" behaviour.
  private val cancelProgress = Animatable(0f).apply { updateBounds(0f, 1f) }
  private val completionProgress = Animatable(0f).apply { updateBounds(0f, 1f) }

  /** Touch position the gesture started from; the y-shift is measured against it. */
  private var initialTouchY: Float = Float.NaN
  private var currentTouchY: Float = Float.NaN
  private var committedTouchY: Float = Float.NaN

  /**
   * Rolling velocity of the eased gesture progress, in progress-per-second,
   * sampled from the navigation event's frame timestamps so the settle spring
   * can pick the gesture up where the finger left it.
   */
  private var progressVelocity: Float = 0f
  private var lastSampleProgress: Float = 0f
  private var lastSampleTimeMillis: Long = 0L

  internal fun updateRuntime(
    gestureProgressProvider: () -> Float,
    maxScrimAlpha: Float,
  ) {
    this.gestureProgressProvider = gestureProgressProvider
    this.maxScrimAlpha = maxScrimAlpha
  }

  internal fun startGesture(
    closingKey: Any,
    enteringKey: Any?,
    swipeEdge: Int,
    touchY: Float,
  ) {
    this.swipeEdge = swipeEdge
    if (
      phase == Phase.GESTURE &&
      this.closingKey == closingKey &&
      this.enteringKey == enteringKey
    ) {
      return
    }
    this.closingKey = closingKey
    this.enteringKey = enteringKey
    lastProgress = 0f
    committedStartProgress = 0f
    initialTouchY = touchY
    currentTouchY = touchY
    committedTouchY = touchY
    progressVelocity = 0f
    lastSampleProgress = 0f
    lastSampleTimeMillis = 0L
    phase = Phase.GESTURE
  }

  internal fun updateGestureProgress(
    progress: Float,
    swipeEdge: Int,
    touchY: Float,
    frameTimeMillis: Long,
  ): Float {
    this.swipeEdge = swipeEdge
    val easedProgress = easedGestureProgress(progress)
    if (initialTouchY.isNaN()) initialTouchY = touchY
    currentTouchY = touchY
    sampleVelocity(easedProgress, frameTimeMillis)
    lastProgress = easedProgress
    return easedProgress
  }

  /**
   * Records progress-per-second between two distinct event frames. Same-frame
   * reads are ignored: the provider is polled once per placement pass, which
   * can happen several times per frame, and a zero time delta would otherwise
   * wipe the velocity right before the finger lifts.
   */
  private fun sampleVelocity(easedProgress: Float, frameTimeMillis: Long) {
    if (lastSampleTimeMillis == 0L || frameTimeMillis <= lastSampleTimeMillis) {
      if (lastSampleTimeMillis == 0L) {
        lastSampleTimeMillis = frameTimeMillis
        lastSampleProgress = easedProgress
      }
      return
    }
    val elapsedSeconds = (frameTimeMillis - lastSampleTimeMillis) / 1000f
    progressVelocity = (easedProgress - lastSampleProgress) / elapsedSeconds
    lastSampleProgress = easedProgress
    lastSampleTimeMillis = frameTimeMillis
  }

  internal fun cancel() {
    if (phase == Phase.IDLE) return
    scope.launch {
      val startProgress = lastProgress.coerceIn(0f, 1f)
      // Only a finger still travelling backwards should speed the cancel up;
      // carrying a forward velocity over would make the surface lurch further
      // out before returning.
      val handoffVelocity = progressVelocity.coerceAtMost(0f)
      phase = Phase.CANCEL
      cancelProgress.snapTo(startProgress)
      cancelProgress.animateTo(
        targetValue = 0f,
        animationSpec = settleSpring(),
        initialVelocity = handoffVelocity,
      )
      clear()
    }
  }

  internal fun complete() {
    if (phase == Phase.IDLE) return
    val startProgress = lastProgress.coerceIn(0f, 1f)
    // Mirror of cancel(): a finger travelling backwards at release must not
    // push the committed animation forwards.
    val handoffVelocity = progressVelocity.coerceAtLeast(0f)
    phase = Phase.COMPLETE
    committedStartProgress = startProgress
    committedTouchY = currentTouchY
    scope.launch {
      completionProgress.snapTo(0f)
      completionProgress.animateTo(
        targetValue = 1f,
        animationSpec = settleSpring(),
        // The gesture measures progress towards the commit point while this
        // animation measures progress away from it, so the remainder of the
        // gesture sets the initial scale.
        initialVelocity = handoffVelocity / max(1f - startProgress, MIN_VELOCITY_SCALE),
      )
      clear()
    }
  }

  private fun settleSpring() = spring<Float>(
    dampingRatio = spec.settleDampingRatio,
    stiffness = spec.settleStiffness,
    visibilityThreshold = SETTLE_VISIBILITY_THRESHOLD,
  )

  private fun clear() {
    phase = Phase.IDLE
    closingKey = null
    enteringKey = null
    lastProgress = 0f
    committedStartProgress = 0f
    swipeEdge = NavigationEvent.EDGE_LEFT
    initialTouchY = Float.NaN
    currentTouchY = Float.NaN
    committedTouchY = Float.NaN
    progressVelocity = 0f
    lastSampleProgress = 0f
    lastSampleTimeMillis = 0L
  }

  internal fun roleFor(sceneKey: Any): SceneRole {
    if (phase == Phase.IDLE) return SceneRole.NONE
    return when (sceneKey) {
      closingKey -> SceneRole.CLOSING
      enteringKey -> SceneRole.ENTERING
      else -> SceneRole.NONE
    }
  }

  internal fun transformFor(
    role: SceneRole,
    widthPx: Float,
    heightPx: Float,
    enteringOffsetPx: Float,
    marginPx: Float,
  ): Motion3Transform = when (phase) {
    Phase.IDLE -> Motion3Transform()

    Phase.GESTURE,
    Phase.CANCEL,
    -> preCommitTransform(
      role = role,
      progress = preCommitProgress(),
      widthPx = widthPx,
      heightPx = heightPx,
      enteringOffsetPx = enteringOffsetPx,
      marginPx = marginPx,
      swipeEdge = swipeEdge,
      touchY = currentTouchY,
    )

    Phase.COMPLETE -> postCommitTransform(
      role = role,
      startProgress = committedStartProgress.coerceIn(0f, 1f),
      completionProgress = completionProgress.value.coerceIn(0f, 1f),
      widthPx = widthPx,
      heightPx = heightPx,
      enteringOffsetPx = enteringOffsetPx,
      marginPx = marginPx,
      swipeEdge = swipeEdge,
    )
  }

  internal fun scrimAlpha(): Float {
    val scrimProgress = when (phase) {
      Phase.IDLE -> 0f

      Phase.GESTURE,
      Phase.CANCEL,
      -> preCommitProgress()

      Phase.COMPLETE -> 1f - completionProgress.value.coerceIn(0f, 1f)
    }
    return maxScrimAlpha * scrimProgress.coerceIn(0f, 1f)
  }

  private fun preCommitProgress(): Float = when (phase) {
    Phase.GESTURE -> gestureProgressProvider()
    Phase.CANCEL -> cancelProgress.value
    Phase.COMPLETE -> committedStartProgress
    Phase.IDLE -> 0f
  }.coerceIn(0f, 1f)

  /**
   * Vertical offset the surface takes on top of being centred, following the
   * finger. The shift is capped so the surface never crosses the margin, and the
   * travelled distance is mapped onto that cap through a decelerating curve;
   * scaled by progress so the surface does not jump as soon as the gesture is
   * recognised.
   */
  private fun gestureYShift(
    heightPx: Float,
    marginPx: Float,
    progress: Float,
    touchY: Float,
  ): Float {
    if (!spec.followTouchVertically) return 0f
    if (touchY.isNaN() || initialTouchY.isNaN()) return 0f
    val maxYShift = heightPx * spec.maxYShiftFraction - marginPx
    if (maxYShift <= 0f) return 0f
    val rawDelta = touchY - initialTouchY
    val ratio = (abs(rawDelta) / maxYShift).coerceIn(0f, 1f)
    return spec.verticalEasing.transform(ratio) * maxYShift * sign(rawDelta) * progress
  }

  private fun preCommitTransform(
    role: SceneRole,
    progress: Float,
    widthPx: Float,
    heightPx: Float,
    enteringOffsetPx: Float,
    marginPx: Float,
    swipeEdge: Int,
    touchY: Float,
  ): Motion3Transform {
    if (role == SceneRole.NONE) return Motion3Transform()
    val scale = lerp(1f, spec.maxScale, progress)
    val translationX = when (role) {
      SceneRole.CLOSING -> lerp(
        start = 0f,
        stop = closingTargetTranslationX(widthPx, marginPx, swipeEdge),
        fraction = progress,
      )

      SceneRole.ENTERING -> lerp(
        start = -enteringOffsetPx,
        stop = -enteringOffsetPx + centeredScaleInset(widthPx),
        fraction = progress,
      )

      SceneRole.NONE -> 0f
    }
    val centredTranslationY = lerp(
      start = 0f,
      stop = centeredScaleInset(heightPx),
      fraction = progress,
    )
    // Only the closing surface tracks the finger; the entering surface stays
    // put underneath, the way the platform animates a returning task.
    val yShift = if (role == SceneRole.CLOSING) {
      gestureYShift(heightPx, marginPx, progress, touchY)
    } else {
      0f
    }
    return Motion3Transform(
      translationX = translationX,
      translationY = centredTranslationY + yShift,
      scale = scale,
      clip = progress > 0.001f,
    )
  }

  private fun postCommitTransform(
    role: SceneRole,
    startProgress: Float,
    completionProgress: Float,
    widthPx: Float,
    heightPx: Float,
    enteringOffsetPx: Float,
    marginPx: Float,
    swipeEdge: Int,
  ): Motion3Transform {
    if (role == SceneRole.NONE) return Motion3Transform()
    val startScale = lerp(1f, spec.maxScale, startProgress)
    val startTranslationX = when (role) {
      SceneRole.CLOSING -> lerp(
        start = 0f,
        stop = closingTargetTranslationX(widthPx, marginPx, swipeEdge),
        fraction = startProgress,
      )

      SceneRole.ENTERING -> lerp(
        start = -enteringOffsetPx,
        stop = -enteringOffsetPx + centeredScaleInset(widthPx),
        fraction = startProgress,
      )

      SceneRole.NONE -> 0f
    }
    // Hand the settle animation the exact vertical offset the finger left the
    // surface at, so the committed animation continues from where the gesture
    // stopped instead of snapping back to centre first.
    val startYShift = if (role == SceneRole.CLOSING) {
      gestureYShift(heightPx, marginPx, startProgress, committedTouchY)
    } else {
      0f
    }
    val startTranslationY = lerp(
      start = 0f,
      stop = centeredScaleInset(heightPx),
      fraction = startProgress,
    ) + startYShift
    val targetTranslationX = when (role) {
      SceneRole.CLOSING -> startTranslationX + enteringOffsetPx
      SceneRole.ENTERING -> 0f
      SceneRole.NONE -> 0f
    }
    val alpha = when (role) {
      SceneRole.CLOSING -> max(1f - completionProgress * 2f, 0f)

      SceneRole.ENTERING,
      SceneRole.NONE,
      -> 1f
    }
    return Motion3Transform(
      translationX = lerp(startTranslationX, targetTranslationX, completionProgress),
      translationY = lerp(startTranslationY, 0f, completionProgress),
      scale = lerp(startScale, 1f, completionProgress),
      alpha = alpha,
      clip = completionProgress < 1f,
    )
  }

  private fun closingTargetTranslationX(
    widthPx: Float,
    marginPx: Float,
    swipeEdge: Int,
  ): Float = if (swipeEdge == NavigationEvent.EDGE_RIGHT) {
    centeredScaleInset(widthPx)
  } else {
    widthPx * (1f - spec.maxScale) - marginPx
  }

  private fun centeredScaleInset(widthPx: Float): Float = widthPx * (1f - spec.maxScale) / 2f

  private fun easedGestureProgress(progress: Float): Float = spec.gestureEasing.transform(progress.coerceIn(0f, 1f))

  private companion object {
    const val SETTLE_VISIBILITY_THRESHOLD = 0.001f
    const val MIN_VELOCITY_SCALE = 0.05f
  }
}

internal enum class Phase {
  IDLE,
  GESTURE,
  CANCEL,
  COMPLETE,
}

internal enum class SceneRole {
  NONE,
  CLOSING,
  ENTERING,
}

internal data class Motion3Transform(
  val translationX: Float = 0f,
  val translationY: Float = 0f,
  val scale: Float = 1f,
  val alpha: Float = 1f,
  val clip: Boolean = false,
)

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction.coerceIn(0f, 1f)
