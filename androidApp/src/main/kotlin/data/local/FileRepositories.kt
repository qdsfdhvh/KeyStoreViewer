package data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

/**
 * 简单文件存储实现(M2 数据量级足够;后续如需查询能力可迁移 Room)。
 * 写入均为先内存后落盘,调用方在 IO 线程调用。
 */
class FileHistoryRepository(
  context: Context,
) : HistoryRepository {

  private val file = File(context.filesDir, "history.json")
  private val maxEntries = 500

  private val state = MutableStateFlow(
    runCatching {
      if (file.exists()) {
        json.decodeFromString(ListSerializer(HistoryEntry.serializer()), file.readText())
      } else {
        emptyList()
      }
    }.getOrDefault(emptyList()),
  )

  override val entries = state.asStateFlow()

  private fun persist(entries: List<HistoryEntry>) {
    runCatching {
      file.writeText(json.encodeToString(entries))
    }
  }

  override suspend fun record(packageName: String, displayName: String) {
    state.update { current ->
      val others = current.filterNot { it.packageName == packageName }
      val entry = HistoryEntry(
        packageName = packageName,
        displayName = displayName,
        viewedAtMillis = System.currentTimeMillis(),
      )
      persist(listOf(entry) + others)
      (listOf(entry) + others).take(maxEntries)
    }
  }

  override suspend fun remove(packageName: String) {
    state.update { current ->
      val next = current.filterNot { it.packageName == packageName }
      persist(next)
      next
    }
  }

  override suspend fun clear() {
    state.update {
      persist(emptyList())
      emptyList()
    }
  }
}

class FileFavoritesRepository(
  context: Context,
) : FavoritesRepository {

  private val file = File(context.filesDir, "favorites.json")

  private val state = MutableStateFlow(
    runCatching {
      if (file.exists()) {
        json.decodeFromString(ListSerializer(FavoriteEntry.serializer()), file.readText())
      } else {
        emptyList()
      }
    }.getOrDefault(emptyList()),
  )

  override val entries = state.asStateFlow()

  private fun persist(entries: List<FavoriteEntry>) {
    runCatching {
      file.writeText(json.encodeToString(entries))
    }
  }

  override suspend fun toggle(packageName: String, displayName: String) {
    state.update { current ->
      if (current.any { it.packageName == packageName }) {
        val next = current.filterNot { it.packageName == packageName }
        persist(next)
        next
      } else {
        val entry = FavoriteEntry(
          packageName = packageName,
          displayName = displayName,
          addedAtMillis = System.currentTimeMillis(),
        )
        val next = listOf(entry) + current
        persist(next)
        next
      }
    }
  }

  override suspend fun contains(packageName: String): Boolean = state.value.any { it.packageName == packageName }

  override suspend fun remove(packageName: String) {
    state.update { current ->
      val next = current.filterNot { it.packageName == packageName }
      persist(next)
      next
    }
  }
}
