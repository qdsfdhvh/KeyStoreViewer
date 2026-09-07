package signature

import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

/**
 * Deterministic regression tests for the file-backed monitor state storage:
 * atomic rollback-safe writes (old state preserved on failure), propagated
 * persistence failures, corrupt-file recovery and deletion.
 */
class MonitorFileStateStorageTest {

  private val fileSystem = FileSystem.SYSTEM
  private val dir: Path =
    FileSystem.SYSTEM_TEMPORARY_DIRECTORY / ("monitor-store-test-" + Random.nextInt().toUInt().toString(16))

  init {
    fileSystem.createDirectories(dir)
  }

  @After
  fun cleanup() {
    fileSystem.deleteRecursively(dir)
  }

  private fun storage(): MonitorFileStateStorage = MonitorFileStateStorage(dir / "state.json", fileSystem)

  @Test
  fun transactPersistsAndLoadRoundTrips() = runBlocking {
    val store = storage()
    val committed = store.transact { current ->
      current.copy(enabled = true) to current.enabled
    }
    assertEquals(false, committed)
    assertEquals(MonitorState(enabled = true), store.load())
    assertEquals(
      "no temporary file remains",
      listOf("state.json"),
      fileSystem.list(dir).map { it.name },
    )
  }

  @Test
  fun writeFailurePropagatesAndPreservesPreviousState() = runBlocking {
    val path = dir / "state.json"
    val store = MonitorFileStateStorage(path, fileSystem)
    val first = MonitorState(enabled = true, lastScanAtMillis = 42L)
    store.transact { first to Unit }
    assertEquals(first, store.load())

    val failing = object : ForwardingFileSystem(fileSystem) {
      override fun sink(file: Path, mustExist: Boolean): okio.Sink {
        if (file.name.endsWith(".tmp")) {
          throw IOException("simulated disk full")
        }
        return super.sink(file, mustExist)
      }
    }
    val failingStore = MonitorFileStateStorage(path, failing)
    val second = MonitorState(enabled = false, lastScanAtMillis = 99L)
    try {
      failingStore.transact { second to Unit }
      fail("write failure must propagate")
    } catch (expected: IOException) {
      assertEquals("simulated disk full", expected.message)
    }
    assertEquals("old state must survive a failed write", first, store.load())
    assertTrue(
      "partial temp files must be cleaned up",
      fileSystem.list(dir).map { it.name } == listOf("state.json"),
    )
  }

  @Test
  fun atomicMoveFailurePropagatesWithoutDestroyingOldState() = runBlocking {
    val path = dir / "state.json"
    val store = MonitorFileStateStorage(path, fileSystem)
    val first = MonitorState(enabled = true, lastScanAtMillis = 7L)
    store.transact { first to Unit }

    val failing = object : ForwardingFileSystem(fileSystem) {
      override fun atomicMove(source: Path, target: Path) {
        if (target == path) {
          throw IOException("simulated move failure")
        }
        super.atomicMove(source, target)
      }
    }
    try {
      MonitorFileStateStorage(path, failing).transact { MonitorState(lastScanAtMillis = 8L) to Unit }
      fail("move failure must propagate")
    } catch (expected: IOException) {
      assertEquals("simulated move failure", expected.message)
    }
    assertEquals("old state must survive a failed move", first, store.load())
    assertEquals(
      "temp file removed after failed move",
      listOf("state.json"),
      fileSystem.list(dir).map { it.name },
    )
  }

  @Test
  fun initialScanReadFailurePreservesBytesAndDoesNotNotify() = scanFailure(failRead = 1)

  @Test
  fun transactionSecondReadFailurePreservesBytesAndDoesNotNotify() = scanFailure(failRead = 2)

  @Test
  fun scanCommitFailurePreservesBytesAndDoesNotNotify() = scanFailure(failCommit = true)

  private fun scanFailure(failRead: Int = 0, failCommit: Boolean = false) = runBlocking {
    val path = dir / "state.json"
    val store = storage()
    val baseline = applyMonitorScan(
      MonitorState(enabled = true),
      mapOf("app" to SignerSnapshot("app", setOf("AA"), 1)),
      1,
    ).newState
    store.transact { baseline to Unit }
    val before = fileSystem.read(path) { readByteString() }
    var reads = 0
    var collections = 0
    var notifications = 0
    val failing = object : ForwardingFileSystem(fileSystem) {
      override fun source(file: Path): okio.Source {
        if (file == path && ++reads == failRead) throw IOException("read failure")
        return super.source(file)
      }

      override fun atomicMove(source: Path, target: Path) {
        if (failCommit) throw IOException("commit failure")
        super.atomicMove(source, target)
      }
    }
    val engine = MonitorScanEngine { MonitorFileStateStorage(path, failing) }
    try {
      engine.scan(
        collect = {
          collections++
          mapOf("app" to SignerSnapshot("app", setOf("BB"), 2))
        },
        scannedAtMillis = 2,
        allowNotifications = true,
        notifyNewAlerts = { notifications++ },
      )
      fail("scan IO failure must propagate")
    } catch (_: IOException) {
      // Expected: neither the initial nor the transaction read can commit.
    }
    assertEquals(if (failRead == 1) 0 else 1, collections)
    assertEquals(0, notifications)
    assertEquals(before, fileSystem.read(path) { readByteString() })
    assertEquals(baseline, store.load())
    assertEquals(listOf(path), fileSystem.list(dir))
  }

  @Test
  fun corruptStateFileRecoversAsFreshState() = runBlocking {
    val path = dir / "state.json"
    fileSystem.write(path) { writeUtf8("{ this is not json !!!") }
    val store = MonitorFileStateStorage(path, fileSystem)
    val loaded = store.load()
    assertEquals(MonitorState(), loaded)
    assertFalse("corrupt state must not look enabled", loaded.enabled)
  }

  @Test
  fun missingFileLoadsAsFreshState() = runBlocking {
    val store = storage()
    assertEquals(MonitorState(), store.load())
  }

  @Test
  fun deleteAllRemovesTheStateFile() = runBlocking {
    val store = storage()
    store.transact { MonitorState(enabled = true) to Unit }
    assertTrue(fileSystem.exists(dir / "state.json"))
    store.deleteAll()
    assertFalse(fileSystem.exists(dir / "state.json"))
    assertEquals(MonitorState(), store.load())
  }

  @Test
  fun serializedMonitorStateRoundTripsThroughTheRealJsonCodec() = runBlocking {
    val store = storage()
    val baseline = applyMonitorScan(
      MonitorState(enabled = true),
      mapOf("app.a" to SignerSnapshot("app.a", setOf("AA1"), 1)),
      1000,
    ).newState
    val rotated = applyMonitorScan(
      baseline,
      mapOf("app.a" to SignerSnapshot("app.a", setOf("ZZ1"), 2)),
      2000,
    ).newState
    assertTrue(rotated.known.isNotEmpty())
    store.transact { rotated to Unit }
    assertEquals(rotated, store.load())
  }
}
