package com.seiko.keystoreviewer

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.seiko.keystoreviewer.di.AppGraph
import com.seiko.keystoreviewer.di.AppGraphProvider
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import signature.MonitorStateStore

/**
 * Owns the lazily-created, process-wide Metro graph. Created on first access
 * (MainActivity), so graph construction never delays process start; the graph
 * is bound to this Application's lifetime, never to an Activity.
 *
 * WorkManager also initializes on demand: this class implements
 * [Configuration.Provider] and the manifest removes WorkManagerInitializer,
 * so an ordinary process start no longer pays WorkManager setup while the
 * opt-in signature monitor (the only WorkManager user) is off.
 */
class KeyStoreViewerApplication :
  Application(),
  AppGraphProvider,
  Configuration.Provider {

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().build()

  override val appGraph: AppGraph by lazy {
    createGraphFactory<AppGraph.Factory>().create(this)
  }

  private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    reArmSignatureMonitorIfNeeded()
  }

  /**
   * A force stop cancels WorkManager's jobs; eager initialization used to
   * re-enqueue the persisted monitor work on the next process start (its
   * ForceStopRunnable). With on-demand initialization that only happens when
   * something touches [WorkManager], so touch it here - but only when the
   * durable monitor state says the monitor is enabled. The state read is a
   * small, missing-file-tolerant JSON parse on a background thread. Best
   * effort by design: storage or init failures must never break app startup,
   * and the next monitor enable re-enqueues the unique periodic work anyway.
   */
  private fun reArmSignatureMonitorIfNeeded() {
    val appContext = applicationContext
    startupScope.launch {
      val monitorEnabled = runCatching { MonitorStateStore.get(appContext).load().enabled }
        .onFailure { Log.w(TAG, "Cannot read signature monitor state", it) }
        .getOrDefault(false)
      if (monitorEnabled) {
        runCatching { WorkManager.getInstance(appContext) }
          .onFailure { Log.w(TAG, "Cannot re-arm signature monitor work", it) }
      }
    }
  }

  private companion object {
    const val TAG = "KeyStoreViewerApp"
  }
}
