package data.local

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

@Serializable
data class HistoryEntry(
  val packageName: String,
  val displayName: String,
  val viewedAtMillis: Long,
)

interface HistoryRepository {
  val entries: Flow<List<HistoryEntry>>

  suspend fun record(packageName: String, displayName: String)

  suspend fun remove(packageName: String)

  suspend fun clear()
}

@Serializable
data class FavoriteEntry(
  val packageName: String,
  val displayName: String,
  val addedAtMillis: Long,
)

interface FavoritesRepository {
  /** Ordered by latest added first. */
  val entries: Flow<List<FavoriteEntry>>

  suspend fun toggle(packageName: String, displayName: String)

  suspend fun contains(packageName: String): Boolean

  suspend fun remove(packageName: String)
}

/**
 * 导出次数配额。免费额度按天重置,看激励广告可获得额外次数。
 * 无广告能力的构建(F-Droid / GitHub Release / Desktop)默认 [UnlimitedExportQuota]:
 * 功能完全免费,与激励广告的解锁逻辑天然兼容。
 */
interface ExportQuota {
  val remaining: Flow<Int>

  /** @return true 表示消耗一次成功,允许导出 */
  suspend fun tryConsume(): Boolean

  suspend fun addBonus(count: Int)
}

data object UnlimitedExportQuota : ExportQuota {
  override val remaining: Flow<Int> = flowOf(Int.MAX_VALUE)

  override suspend fun tryConsume(): Boolean = true

  override suspend fun addBonus(count: Int) = Unit
}

data object EmptyHistoryRepository : HistoryRepository {
  override val entries: Flow<List<HistoryEntry>> = flowOf(emptyList())

  override suspend fun record(packageName: String, displayName: String) = Unit

  override suspend fun remove(packageName: String) = Unit

  override suspend fun clear() = Unit
}

data object EmptyFavoritesRepository : FavoritesRepository {
  override val entries: Flow<List<FavoriteEntry>> = flowOf(emptyList())

  override suspend fun toggle(packageName: String, displayName: String) = Unit

  override suspend fun contains(packageName: String): Boolean = false

  override suspend fun remove(packageName: String) = Unit
}

val LocalHistoryRepository = staticCompositionLocalOf<HistoryRepository> { EmptyHistoryRepository }

val LocalFavoritesRepository = staticCompositionLocalOf<FavoritesRepository> { EmptyFavoritesRepository }

val LocalExportQuota = staticCompositionLocalOf<ExportQuota> { UnlimitedExportQuota }
