package com.seiko.keystoreviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import data.model.SignSource
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
          val backStack = rememberNavBackStack(AppList)
          NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
              entry<AppList> {
                AppListScreen(
                  onItemClick = { signSource ->
                    backStack.add(SignatureDetail(signSource))
                  },
                )
              }
              entry<SignatureDetail> { key ->
                SignatureDetailScreen(
                  signSource = key.signSource,
                  onBack = { backStack.removeLastOrNull() },
                )
              }
            },
          )
        }
      }
    }
  }
}

data object AppList : NavKey

data class SignatureDetail(val signSource: SignSource) : NavKey
