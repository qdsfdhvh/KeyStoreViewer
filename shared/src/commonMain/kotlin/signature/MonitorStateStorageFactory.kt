package signature

import okio.FileSystem
import okio.IOException
import okio.Path

/** Verified legacy cleanup before exposing/caching the no-backup store. */
class MonitorStateStorageFactory(
  private val target: Path,
  private val legacy: Path,
  private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
  private var instance: MonitorStateStorage? = null

  @Synchronized
  fun get(): MonitorStateStorage {
    if (fileSystem.exists(legacy)) {
      if (!fileSystem.exists(target)) {
        fileSystem.atomicMove(legacy, target)
        if (!fileSystem.exists(target)) {
          throw IOException("Monitor state migration did not create its target")
        }
      } else {
        fileSystem.delete(legacy)
      }
      verifyLegacyRemoved()
    }
    return instance ?: object : MonitorStateStorage by MonitorFileStateStorage(target, fileSystem) {
      override suspend fun deleteAll() {
        // Remove legacy first: never delete the disabled target while an old
        // enabled copy could still be migrated on process restart.
        fileSystem.delete(legacy, mustExist = false)
        verifyLegacyRemoved()
        fileSystem.delete(target, mustExist = false)
        if (fileSystem.exists(target)) {
          throw IOException("Monitor state was not deleted")
        }
      }
    }.also { instance = it }
  }

  private fun verifyLegacyRemoved() {
    if (fileSystem.exists(legacy)) {
      throw IOException("Legacy monitor state was not removed")
    }
  }
}
