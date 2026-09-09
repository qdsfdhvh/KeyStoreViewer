package ui.screen

import data.local.FavoriteEntry
import data.local.FavoritesRepository
import data.local.HistoryEntry
import data.local.HistoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeHistoryRepository : HistoryRepository {
  override val entries = MutableStateFlow<List<HistoryEntry>>(emptyList())

  /** When set, every write suspends until completed (test gate for cancellation races). */
  var gate: CompletableDeferred<Unit>? = null

  val recorded = mutableListOf<String>()
  val removed = mutableListOf<String>()
  var clearCount = 0

  override suspend fun record(packageName: String, displayName: String) {
    gate?.await()
    recorded += packageName
    entries.update { current ->
      listOf(HistoryEntry(packageName, displayName, 0L)) + current.filterNot { it.packageName == packageName }
    }
  }

  override suspend fun remove(packageName: String) {
    gate?.await()
    removed += packageName
    entries.update { current -> current.filterNot { it.packageName == packageName } }
  }

  override suspend fun clear() {
    gate?.await()
    clearCount++
    entries.value = emptyList()
  }
}

class FakeFavoritesRepository : FavoritesRepository {
  override val entries = MutableStateFlow<List<FavoriteEntry>>(emptyList())

  /** When set, every write suspends until completed (test gate for cancellation races). */
  var gate: CompletableDeferred<Unit>? = null

  val toggled = mutableListOf<String>()
  val removed = mutableListOf<String>()

  override suspend fun toggle(packageName: String, displayName: String) {
    gate?.await()
    toggled += packageName
    entries.update { current ->
      if (current.any { it.packageName == packageName }) {
        current.filterNot { it.packageName == packageName }
      } else {
        listOf(FavoriteEntry(packageName, displayName, 0L)) + current
      }
    }
  }

  override suspend fun contains(packageName: String): Boolean = entries.value.any { it.packageName == packageName }

  override suspend fun remove(packageName: String) {
    gate?.await()
    removed += packageName
    entries.update { current -> current.filterNot { it.packageName == packageName } }
  }
}
