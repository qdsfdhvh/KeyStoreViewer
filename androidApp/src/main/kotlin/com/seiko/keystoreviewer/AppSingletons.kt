package com.seiko.keystoreviewer

import android.content.Context
import com.seiko.keystoreviewer.ads.ExportQuotaProvider
import data.local.ExportQuota
import data.local.FileFavoritesRepository
import data.local.FileHistoryRepository

/**
 * Process-scoped instances of the stateful app-data providers. The providers
 * hold in-memory state seeded from disk, and entry-scoped ViewModels retain
 * the captured instance for the entry's lifetime, so a new instance per
 * recomposition or Activity recreation would leave screens and their
 * ViewModels on diverging in-memory states. One instance per process keeps
 * every consumer on the same state.
 */
object AppSingletons {

  private var history: FileHistoryRepository? = null
  private var favorites: FileFavoritesRepository? = null
  private var exportQuota: ExportQuota? = null

  @Synchronized
  fun history(context: Context): FileHistoryRepository = history ?: FileHistoryRepository(context).also { history = it }

  @Synchronized
  fun favorites(context: Context): FileFavoritesRepository = favorites ?: FileFavoritesRepository(context).also { favorites = it }

  @Synchronized
  fun exportQuota(context: Context): ExportQuota = exportQuota ?: ExportQuotaProvider.quota(context).also { exportQuota = it }
}
