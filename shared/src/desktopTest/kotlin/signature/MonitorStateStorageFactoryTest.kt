package signature

import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.nio.file.Files

class MonitorStateStorageFactoryTest {
  private val fs = FileSystem.SYSTEM
  private val dir = Files.createTempDirectory("monitor-migration").toFile().toOkioPath()
  private val legacy = dir / "legacy.json"
  private val target = dir / "no-backup.json"

  @After
  fun cleanup() = fs.deleteRecursively(dir)

  private suspend fun seed(path: Path, enabled: Boolean = true) {
    MonitorFileStateStorage(path).transact { MonitorState(enabled = enabled) to Unit }
  }

  @Test
  fun failedMoveIsNotCachedAndRepeatedGetRetries() = runBlocking {
    seed(legacy)
    val before = fs.read(legacy) { readByteString() }
    var fail = true
    var attempts = 0
    val io = object : ForwardingFileSystem(fs) {
      override fun atomicMove(source: Path, target: Path) {
        attempts++
        if (fail) throw IOException("move failed")
        super.atomicMove(source, target)
      }
    }
    val factory = MonitorStateStorageFactory(target, legacy, io)
    repeat(2) {
      expectIo { factory.get() }
      assertEquals(before, fs.read(legacy) { readByteString() })
      assertFalse(fs.exists(target))
    }
    fail = false
    val store = factory.get()
    assertTrue(store.load().enabled)
    assertSame(store, factory.get())
    assertEquals(3, attempts)
    assertFalse(fs.exists(legacy))
    MonitorScanEngine { factory.get() }.disable {}
    assertFalse(fs.exists(target))
    assertFalse(MonitorStateStorageFactory(target, legacy).get().load().enabled)
  }

  @Test
  fun bothCopiesFailedDeleteOrSilentNoOpFailsThenRetryKeepsCurrent() = runBlocking {
    for (silent in listOf(false, true)) {
      seed(legacy)
      seed(target, enabled = false)
      var fail = true
      val io = object : ForwardingFileSystem(fs) {
        override fun delete(path: Path, mustExist: Boolean) {
          if (path == legacy && fail) {
            if (silent) return
            throw IOException("delete failed")
          }
          super.delete(path, mustExist)
        }
      }
      val factory = MonitorStateStorageFactory(target, legacy, io)
      repeat(2) { expectIo { factory.get() } }
      assertTrue(fs.exists(legacy))
      assertFalse(MonitorFileStateStorage(target).load().enabled)
      fail = false
      val store = factory.get()
      assertFalse(store.load().enabled)
      assertFalse(fs.exists(legacy))
      MonitorScanEngine { factory.get() }.disable {}
      assertFalse(fs.exists(target))
      assertFalse(MonitorStateStorageFactory(target, legacy).get().load().enabled)
    }
  }

  @Test
  fun cachedStoreDisableCannotSucceedWithLegacyLeftAndRestartCannotResurrect() = runBlocking {
    seed(target)
    var failDelete = false
    val io = object : ForwardingFileSystem(fs) {
      override fun delete(path: Path, mustExist: Boolean) {
        if (path == legacy && failDelete) return // Models File.delete(false).
        super.delete(path, mustExist)
      }
    }
    val factory = MonitorStateStorageFactory(target, legacy, io)
    val cached = factory.get()
    val engine = MonitorScanEngine { cached }
    seed(legacy)
    failDelete = true
    expectIo { engine.disable {} }
    assertFalse(MonitorFileStateStorage(target).load().enabled)
    assertTrue(fs.exists(legacy))
    expectIo { factory.get() } // Even cached get revalidates cleanup.
    expectIo { MonitorStateStorageFactory(target, legacy, io).get() }
    failDelete = false
    engine.disable {}
    assertFalse(fs.exists(legacy))
    assertFalse(fs.exists(target))
    val restarted = MonitorScanEngine { MonitorStateStorageFactory(target, legacy).get() }
    assertEquals(
      MonitorScanOutcome.MonitorDisabled,
      restarted.scan({ error("must not collect") }, 3, true, { error("must not notify") }),
    )
  }

  private suspend fun expectIo(block: suspend () -> Unit) {
    try {
      block()
      fail("IO failure must propagate")
    } catch (_: IOException) {
      // Expected.
    }
  }
}
