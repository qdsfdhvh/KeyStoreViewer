package signature

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.buffer
import okio.sink
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class TemporaryApkTest {
  private val directory = Files.createTempDirectory("apk-copy-ownership").toFile()

  @After
  fun cleanup() {
    directory.deleteRecursively()
  }

  @Test
  fun repeatedSelectionsDeleteRealCopiesAfterMetadataSuccess() = runBlocking {
    val paths = mutableSetOf<String>()
    repeat(3) {
      val result = withTemporaryApk(directory) { file ->
        assertTrue(paths.add(file.absolutePath))
        file.sink().buffer().use { copyBounded(Buffer().writeUtf8("synthetic apk"), it) }
        assertEquals("synthetic apk", file.readText())
        "metadata only"
      }
      assertEquals("metadata only", result)
      assertTrue(directory.listFiles()!!.isEmpty())
    }
  }

  @Test
  fun copyBoundAndMetadataFailuresDeleteRealPartialFiles() = runBlocking {
    for (tooLarge in listOf(false, true)) {
      try {
        withTemporaryApk(directory) { file ->
          file.sink().buffer().use {
            copyBounded(Buffer().writeUtf8("synthetic apk"), it, if (tooLarge) 2 else 100)
          }
          throw IOException("metadata reader failed")
        }
        fail("failure expected")
      } catch (_: ApkTooLargeException) {
        assertTrue(tooLarge)
      } catch (_: IOException) {
        assertFalse(tooLarge)
      }
      assertTrue(directory.listFiles()!!.isEmpty())
    }
  }

  @Test
  fun cancellationDeletesOwnedFileWithoutDeletingNewSelection() = runBlocking {
    val first = CompletableDeferred<File>()
    val oldRead = launch(start = CoroutineStart.UNDISPATCHED) {
      withTemporaryApk(directory) { file ->
        file.writeText("partial old selection")
        first.complete(file)
        awaitCancellation()
      }
    }
    withTemporaryApk(directory) { current ->
      current.writeText("new selection")
      oldRead.cancel()
      oldRead.join()
      assertFalse(first.await().exists())
      assertTrue(current.exists())
      assertEquals("new selection", current.readText())
    }
    assertTrue(directory.listFiles()!!.isEmpty())
  }
}
