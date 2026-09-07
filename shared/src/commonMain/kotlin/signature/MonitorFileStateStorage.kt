package signature

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * File-backed [MonitorStateStorage].
 *
 * Rollback-safe persistence: the new state is written to a temporary file in
 * the same directory and atomically moved over the target file. A failed
 * write therefore never destroys the previously stored state (the old file is
 * only replaced by a successful move, never deleted first), a concurrent
 * reader never sees a torn file, and every failure propagates to the caller.
 *
 * Concurrency: writers are serialized by the monitor engine's mutex (it is
 * the only writer); reads are safe at any time because of the atomic move.
 */
class MonitorFileStateStorage(
  private val path: Path,
  private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : MonitorStateStorage {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  override suspend fun load(): MonitorState {
    if (!fileSystem.exists(path)) {
      return MonitorState()
    }
    // Filesystem failures must abort transactions, never become disabled state.
    val text = fileSystem.source(path).buffer().use { it.readUtf8() }
    return try {
      json.decodeFromString(MonitorState.serializer(), text)
    } catch (e: SerializationException) {
      // Only malformed JSON recovers as fresh state. Reading alone never
      // overwrites the corrupt file; a later explicit transaction may do so.
      MonitorState()
    }
  }

  override suspend fun <T> transact(block: (MonitorState) -> Pair<MonitorState, T>): T {
    val (next, value) = block(load())
    writeState(next)
    return value
  }

  override suspend fun deleteAll() {
    fileSystem.delete(path, mustExist = false)
  }

  private fun writeState(state: MonitorState) {
    val parent = path.parent ?: error("Monitor state path has no parent directory")
    val temp = parent / (path.name + ".tmp")
    try {
      fileSystem.sink(temp).buffer().use { sink ->
        sink.writeUtf8(json.encodeToString(MonitorState.serializer(), state))
      }
      // Atomic on the same filesystem: readers see the old or the new file,
      // never a partial one. Throws instead of destroying the old state.
      fileSystem.atomicMove(temp, path)
    } catch (e: Exception) {
      runCatching { fileSystem.delete(temp, mustExist = false) }
      throw e
    }
  }
}
