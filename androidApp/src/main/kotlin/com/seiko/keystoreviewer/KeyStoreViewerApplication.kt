package com.seiko.keystoreviewer

import android.app.Application
import com.seiko.keystoreviewer.di.AppGraph
import com.seiko.keystoreviewer.di.AppGraphProvider
import dev.zacsweers.metro.createGraphFactory

/**
 * Owns the lazily-created, process-wide Metro graph. Created on first access
 * (MainActivity), so graph construction never delays process start; the graph
 * is bound to this Application's lifetime, never to an Activity.
 */
class KeyStoreViewerApplication :
  Application(),
  AppGraphProvider {

  override val appGraph: AppGraph by lazy {
    createGraphFactory<AppGraph.Factory>().create(this)
  }
}
