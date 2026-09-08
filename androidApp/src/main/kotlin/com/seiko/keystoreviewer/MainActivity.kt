package com.seiko.keystoreviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberNavigationEventState
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import com.seiko.keystoreviewer.ads.Ads
import com.seiko.keystoreviewer.ui.motion3.Motion3BackHandler
import com.seiko.keystoreviewer.ui.motion3.motion3Metadata
import com.seiko.keystoreviewer.ui.motion3.motion3PopTransitionSpec
import com.seiko.keystoreviewer.ui.motion3.motion3PredictivePopTransitionSpec
import com.seiko.keystoreviewer.ui.motion3.motion3TransitionSpec
import com.seiko.keystoreviewer.ui.motion3.rememberMotion3
import com.seiko.keystoreviewer.ui.motion3.rememberMotion3SceneDecoratorStrategy
import com.seiko.keystoreviewer.update.StoreUpdatePrompt
import data.local.LocalExportQuota
import data.local.LocalFavoritesRepository
import data.local.LocalHistoryRepository
import data.model.SignSource
import kotlinx.serialization.Serializable
import platform.ContentHandler
import platform.LocalContentHandler
import platform.ads.LocalAdSlot
import ui.navigation.RootTab
import ui.navigation.RootTabBar
import ui.navigation.selectRoot
import ui.screen.ApkCompareScreen
import ui.screen.AppListScreen
import ui.screen.FavoritesScreen
import ui.screen.HistoryScreen
import ui.screen.KeystoreBrowserScreen
import ui.screen.SignatureDetailScreen
import ui.screen.SignatureMonitorScreen
import ui.screen.SignatureToolsScreen
import ui.theme.AppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    val contentHandler = ContentHandler(applicationContext)
    Ads.initialize(this)
    setContent {
      AppTheme {
        CompositionLocalProvider(
          LocalContentHandler provides contentHandler,
          LocalAdSlot provides Ads.slot(),
          LocalHistoryRepository provides AppSingletons.history(applicationContext),
          LocalFavoritesRepository provides AppSingletons.favorites(applicationContext),
          LocalExportQuota provides AppSingletons.exportQuota(applicationContext),
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
data object History : NavKey

@Serializable
data object Favorites : NavKey

@Serializable
data object SignatureTools : NavKey

@Serializable
data object KeystoreBrowser : NavKey

@Serializable
data object ApkCompare : NavKey

@Serializable
data object SignatureMonitor : NavKey

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
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      // Scopes viewModel() calls inside entries to the entry's ViewModelStore;
      // cleared by the decorator when the entry is popped.
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<AppList> {
        AppListScreen(
          onItemClick = { signSource ->
            backStack.add(SignatureDetail(signSource))
          },
        )
      }
      entry<History>(
        metadata = motion3Metadata(),
      ) {
        HistoryScreen(
          onOpen = { packageName ->
            backStack.add(SignatureDetail(SignSource.PackageName(packageName)))
          },
        )
      }
      entry<Favorites>(
        metadata = motion3Metadata(),
      ) {
        FavoritesScreen(
          onOpen = { packageName ->
            backStack.add(SignatureDetail(SignSource.PackageName(packageName)))
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
      entry<SignatureTools>(
        metadata = motion3Metadata(),
      ) {
        SignatureToolsScreen(
          onOpenKeystoreBrowser = {
            backStack.add(KeystoreBrowser)
          },
          onOpenApkCompare = {
            backStack.add(ApkCompare)
          },
          onOpenSignatureMonitor = {
            backStack.add(SignatureMonitor)
          },
        )
      }
      entry<KeystoreBrowser>(
        metadata = motion3Metadata(),
      ) {
        KeystoreBrowserScreen(
          onBack = onBack,
        )
      }
      entry<ApkCompare>(
        metadata = motion3Metadata(),
      ) {
        ApkCompareScreen(
          onBack = onBack,
        )
      }
      entry<SignatureMonitor>(
        metadata = motion3Metadata(),
      ) {
        SignatureMonitorScreen(
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

  val selectedRoot = backStack.lastOrNull { it in rootDestinations.values } ?: AppList
  val selectedTab = rootDestinations.entries.first { it.value == selectedRoot }.key
  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      Column {
        StoreUpdatePrompt()
        RootTabBar(selectedTab) { target -> backStack.selectRoot(AppList, rootDestinations.getValue(target)) }
      }
    },
  ) { padding ->
    NavDisplay(
      modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
      sceneState = sceneState,
      navigationEventState = navigationEventState,
      transitionSpec = { motion3TransitionSpec() },
      popTransitionSpec = { motion3PopTransitionSpec() },
      predictivePopTransitionSpec = { motion3PredictivePopTransitionSpec() },
    )
  }
}

private val rootDestinations = mapOf<RootTab, NavKey>(
  RootTab.Apps to AppList,
  RootTab.Tools to SignatureTools,
  RootTab.History to History,
  RootTab.Favorites to Favorites,
)
