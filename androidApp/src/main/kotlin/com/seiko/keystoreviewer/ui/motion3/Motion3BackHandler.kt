package com.seiko.keystoreviewer.ui.motion3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneState
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState

/**
 * Bridges the system predictive-back gesture into [Motion3].
 *
 * While the gesture is in progress the closing surface follows the finger; on
 * lift the motion either springs back ([onBackCancelled]) or commits
 * ([onBackCompleted], invoked once per popped entry). Scenes whose destination
 * did not opt in through [motion3Metadata] are ignored.
 */
@Composable
fun <T : Any> Motion3BackHandler(
  sceneState: SceneState<T>,
  navigationEventState: NavigationEventState<SceneInfo<T>>,
  motion: Motion3,
  maxScrimAlpha: Float = motion.spec.lightScrimAlpha,
  onBackCancelled: () -> Unit = {},
  onBackCompleted: () -> Unit,
) {
  val currentScene = sceneState.currentScene
  val previousScene = sceneState.previousScenes.lastOrNull()
  val motionEnabled = currentScene.metadata[Motion3MetadataKey] == true
  val predictiveBackEnabled = motionEnabled && previousScene != null

  SideEffect {
    motion.updateRuntime(
      gestureProgressProvider = {
        val transition = navigationEventState.transitionState
        if (transition is InProgress && predictiveBackEnabled) {
          motion.updateGestureProgress(
            progress = transition.latestEvent.progress,
            swipeEdge = transition.latestEvent.swipeEdge,
            touchY = transition.latestEvent.touchY,
            frameTimeMillis = transition.latestEvent.frameTimeMillis,
          )
        } else {
          motion.lastProgress
        }
      },
      maxScrimAlpha = maxScrimAlpha,
    )
  }

  LaunchedEffect(
    navigationEventState,
    predictiveBackEnabled,
    motionEnabled,
    currentScene.key,
    previousScene?.key,
  ) {
    snapshotFlow {
      val transition = navigationEventState.transitionState
      if (transition is InProgress && predictiveBackEnabled) {
        GestureSnapshot(
          inProgress = true,
          swipeEdge = transition.latestEvent.swipeEdge,
          touchY = transition.latestEvent.touchY,
        )
      } else {
        GestureSnapshot.Idle
      }
    }.collect { snapshot ->
      if (!snapshot.inProgress) return@collect
      motion.startGesture(
        closingKey = currentScene.key,
        enteringKey = previousScene?.key,
        swipeEdge = snapshot.swipeEdge,
        touchY = snapshot.touchY,
      )
    }
  }

  NavigationBackHandler(
    state = navigationEventState,
    isBackEnabled = currentScene.previousEntries.isNotEmpty(),
    onBackCancelled = {
      if (motionEnabled) {
        motion.cancel()
      }
      onBackCancelled()
    },
    onBackCompleted = {
      if (motionEnabled) {
        motion.complete()
      }
      val popCount = (sceneState.entries.size - currentScene.previousEntries.size)
        .coerceAtLeast(0)
      repeat(popCount) {
        onBackCompleted()
      }
    },
  )
}

private data class GestureSnapshot(
  val inProgress: Boolean,
  @param:NavigationEvent.SwipeEdge val swipeEdge: Int,
  val touchY: Float,
) {
  companion object {
    val Idle = GestureSnapshot(
      inProgress = false,
      swipeEdge = NavigationEvent.EDGE_LEFT,
      touchY = Float.NaN,
    )
  }
}
