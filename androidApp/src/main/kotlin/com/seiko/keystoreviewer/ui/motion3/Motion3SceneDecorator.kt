package com.seiko.keystoreviewer.ui.motion3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy

/**
 * Wraps every scene in a layer that applies the [Motion3] transform
 * (scale, translation, alpha, rounded clip) and draws the predictive-back scrim
 * behind the closing surface.
 */
@Composable
fun <T : Any> rememberMotion3SceneDecoratorStrategy(
  motion: Motion3,
): SceneDecoratorStrategy<T> = remember(motion) {
  SceneDecoratorStrategy { scene ->
    Motion3Scene(scene, motion)
  }
}

private data class Motion3Scene<T : Any>(
  private val scene: Scene<T>,
  private val motion: Motion3,
) : Scene<T> {
  override val key: Any = scene.key
  override val entries: List<NavEntry<T>> = scene.entries
  override val previousEntries: List<NavEntry<T>> = scene.previousEntries
  override val metadata: Map<String, Any> = scene.metadata

  override val content: @Composable () -> Unit = {
    val role = motion.roleFor(key)
    Motion3MotionLayer(
      role = role,
      motion = motion,
    ) {
      scene.content()
    }
  }
}

@Composable
private fun Motion3MotionLayer(
  role: SceneRole,
  motion: Motion3,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val density = LocalDensity.current
  val enteringOffsetPx = with(density) { motion.spec.enteringStartOffset.toPx() }
  val marginPx = with(density) { motion.spec.margin.toPx() }
  val shape = RoundedCornerShape(motion.spec.cornerRadius)
  val contentModifier = Modifier
    .fillMaxSize()
    .layout { measurable, constraints ->
      val placeable = measurable.measure(constraints)
      layout(placeable.width, placeable.height) {
        placeable.placeWithLayer(0, 0) {
          val transform = motion.transformFor(
            role = role,
            widthPx = placeable.width.toFloat(),
            heightPx = placeable.height.toFloat(),
            enteringOffsetPx = enteringOffsetPx,
            marginPx = marginPx,
          )
          translationX = transform.translationX
          translationY = transform.translationY
          scaleX = transform.scale
          scaleY = transform.scale
          alpha = transform.alpha
          transformOrigin = TransformOrigin(0f, 0f)
          clip = transform.clip
          this.shape = shape
        }
      }
    }

  Box(
    modifier = modifier
      .fillMaxSize()
      .drawBehind {
        if (role == SceneRole.CLOSING) {
          val alpha = motion.scrimAlpha()
          if (alpha > 0f) {
            drawRect(Color.Black.copy(alpha = alpha))
          }
        }
      },
  ) {
    Box(modifier = contentModifier) {
      content()
    }
  }
}
