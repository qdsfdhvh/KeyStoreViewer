package com.seiko.keystoreviewer.di

import android.content.Context
import data.local.ExportQuota
import data.local.FavoritesRepository
import data.local.HistoryRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

/**
 * Application-owned Metro graph. Created once per process by
 * [com.seiko.keystoreviewer.KeyStoreViewerApplication]; never per Activity.
 * Owns the app-scoped state ([AppDataBindings]: repositories, export quota)
 * and the aggregated ViewModel factory; ViewModels built through it stay
 * scoped to their Nav3 entries.
 */
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {

  /** Exposed for tests: asserts the app-scoped singleton identity of the bindings. */
  val historyRepository: HistoryRepository

  /** Exposed for tests: asserts the app-scoped singleton identity of the bindings. */
  val favoritesRepository: FavoritesRepository

  /** Exposed for tests: asserts the app-scoped singleton identity of the bindings. */
  val exportQuota: ExportQuota

  @DependencyGraph.Factory
  fun interface Factory {
    /**
     * The application context backs every file/SharedPreferences-backed
     * adapter; tests pass a fake Context here instead of the framework one.
     */
    fun create(@Provides context: Context): AppGraph
  }
}

/** Implemented by the Application so any Context can reach the process-wide graph. */
interface AppGraphProvider {
  val appGraph: AppGraph
}

val Context.appGraph: AppGraph
  get() = (applicationContext as AppGraphProvider).appGraph
