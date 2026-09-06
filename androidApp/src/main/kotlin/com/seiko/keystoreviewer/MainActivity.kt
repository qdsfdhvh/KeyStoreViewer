package com.seiko.keystoreviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import com.seiko.keystoreviewer.ui.motion3.Motion3BackHandler
import com.seiko.keystoreviewer.ui.motion3.motion3Metadata
import com.seiko.keystoreviewer.ui.motion3.motion3PopTransitionSpec
import com.seiko.keystoreviewer.ui.motion3.motion3PredictivePopTransitionSpec
import com.seiko.keystoreviewer.ui.motion3.motion3TransitionSpec
import com.seiko.keystoreviewer.ui.motion3.rememberMotion3
import com.seiko.keystoreviewer.ui.motion3.rememberMotion3SceneDecoratorStrategy
import data.model.SignSource
import kotlinx.serialization.Serializable
import platform.ContentHandler
import platform.LocalContentHandler
import ui.screen.AppListScreen
import ui.screen.SignatureDetailScreen
import ui.theme.AppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val contentHandler = ContentHandler(applicationContext)
    setContent {
      AppTheme {
        CompositionLocalProvider(
          LocalContentHandler provides contentHandler,
        ) {
          KeyStoreViewerApp()
        }
      }
    }
  }
}

@Serializable
data object AppList : NavKey

@Serializable
data class SignatureDetail(val signSource: SignSource) : NavKey

@Composable
private fun KeyStoreViewerApp() {
  val backStack = rememberNavBackStack(AppList)
  val onBack: () -> Unit = {
    if (backStack.size > 1) {
      backStack.removeLastOrNull()
    }
  }

  val motion = rememberMotion3()
  val motionSceneDecorator = rememberMotion3SceneDecoratorStrategy<NavKey>(motion)
  val entries = rememberDecoratedNavEntries(
    backStack = backStack,
    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    entryProvider = entryProvider {
      entry<AppList> {
        AppListScreen(
          onItemClick = { signSource ->
            backStack.add(SignatureDetail(signSource))
          },
        )
      }
      entry<SignatureDetail>(
        metadata = motion3Metadata(),
      ) { key ->
        SignatureDetailScreen(
          signSource = key.signSource,
          onBack = onBack,
        )
      }
    },
  )
  val sceneState = rememberSceneState(
    entries = entries,
    sceneStrategies = listOf(SinglePaneSceneStrategy()),
    sceneDecoratorStrategies = listOf(motionSceneDecorator),
    onBack = onBack,
  )
  val navigationEventState = rememberNavigationEventState(sceneState)

  Motion3BackHandler(
    sceneState = sceneState,
    navigationEventState = navigationEventState,
    motion = motion,
    onBackCompleted = onBack,
  )

  NavDisplay(
    sceneState = sceneState,
    navigationEventState = navigationEventState,
    transitionSpec = { motion3TransitionSpec() },
    popTransitionSpec = { motion3PopTransitionSpec() },
    predictivePopTransitionSpec = { motion3PredictivePopTransitionSpec() },
  )
}
