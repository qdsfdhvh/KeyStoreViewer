package com.seiko.keystoreviewer.di

import android.content.Context
import com.seiko.keystoreviewer.ads.ExportQuotaProvider
import data.local.ExportQuota
import data.local.FavoritesRepository
import data.local.FileFavoritesRepository
import data.local.FileHistoryRepository
import data.local.HistoryRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Process-scoped bindings for the stateful app-data providers. The providers
 * hold in-memory state seeded from disk, and entry-scoped ViewModels retain
 * the captured instance for the entry's lifetime, so a new instance per
 * recomposition or Activity recreation would leave screens and their
 * ViewModels on diverging in-memory states. One instance per process keeps
 * every consumer on the same state (replaces the hand-written AppSingletons).
 *
 * The quota goes through the flavor-specific [ExportQuotaProvider], keeping
 * the FOSS (unlimited) and Play (daily + ad bonus) variants isolated.
 */
@BindingContainer
@ContributesTo(AppScope::class)
object AppDataBindings {

  @Provides
  @SingleIn(AppScope::class)
  fun historyRepository(context: Context): HistoryRepository = FileHistoryRepository(context)

  @Provides
  @SingleIn(AppScope::class)
  fun favoritesRepository(context: Context): FavoritesRepository = FileFavoritesRepository(context)

  @Provides
  @SingleIn(AppScope::class)
  fun exportQuota(context: Context): ExportQuota = ExportQuotaProvider.quota(context)
}
