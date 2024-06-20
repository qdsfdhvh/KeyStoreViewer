package ui.component.navigator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier

@Composable
fun NavHost(
  navigator: Navigator,
  modifier: Modifier = Modifier,
) {
  val saveableStateHolder = rememberSaveableStateHolder()
  CompositionLocalProvider(LocalNavigator provides navigator) {
    AnimatedContent(
      navigator.lastItem,
      transitionSpec = {
        if (navigator.lastEvent == LastEvent.Push) {
          (slideInHorizontally { it }) togetherWith (slideOutHorizontally { -it })
        } else {
          (slideInHorizontally { -it }) togetherWith (slideOutHorizontally { it })
        }
      },
      modifier = modifier,
    ) { entry ->
      saveableStateHolder.SaveableStateProvider(entry.index) {
        entry.screen.Content()
      }
    }
  }
}
