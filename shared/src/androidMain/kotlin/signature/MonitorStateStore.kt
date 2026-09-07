package signature

import android.content.Context
import okio.Path.Companion.toPath
import java.io.File

/**
 * Android factory for the monitor state [MonitorStateStorage].
 *
 * The state file lives in [Context.getNoBackupFilesDir], a directory Android
 * NEVER includes in cloud/auto backups - honoring the monitor's explicit
 * local-only promise. Any legacy copy in [Context.getFilesDir] (which is
 * backup-eligible) is moved into the no-backup directory, or deleted when a
 * current copy already exists, so no snapshot data remains backup-eligible.
 */
object MonitorStateStore {

  private const val FILE_NAME = "signature_monitor_state.json"

  @Volatile
  private var factory: MonitorStateStorageFactory? = null

  fun get(context: Context): MonitorStateStorage = synchronized(this) {
    val appContext = context.applicationContext
    val current = factory ?: MonitorStateStorageFactory(
      target = File(appContext.noBackupFilesDir, FILE_NAME).absolutePath.toPath(),
      legacy = File(appContext.filesDir, FILE_NAME).absolutePath.toPath(),
    )
    // Failed migration is not cached; every call verifies legacy removal.
    current.get().also { factory = current }
  }
}
