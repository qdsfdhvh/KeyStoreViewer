package com.seiko.keystoreviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import platform.ContentHandler
import platform.LocalContentHandler
import ui.screen.AppListScreen
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
          Navigator(AppListScreen) { navigator ->
            SlideTransition(navigator)
          }
        }
      }
    }
  }
}
