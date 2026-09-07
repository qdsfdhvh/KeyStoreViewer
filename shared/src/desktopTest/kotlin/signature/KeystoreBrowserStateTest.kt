package signature

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class KeystoreBrowserStateTest {
  private val success = KeystoreInspection.Success(KeystoreFileFormat.PKCS12, emptyList())

  @Test
  fun delayedBReadCannotParseABytesWithBSelection() = runBlocking {
    val state = KeystoreBrowserState()
    state.read(state.select(), "A") { byteArrayOf(1) }
    state.password = "synthetic-password"
    state.parse { _, _ -> success }
    state.password = "another-synthetic-password"
    val b = state.select() // Actual picker callback transition, before launch.
    assertNull(state.fileName)
    assertNull(state.inspection)
    assertEquals("", state.password)
    assertTrue(state.isReading)
    assertFalse(state.canParse)
    val bytesB = CompletableDeferred<ByteArray>()
    val read = launch(start = CoroutineStart.UNDISPATCHED) { state.read(b, "B") { bytesB.await() } }
    state.parse { _, _ -> error("A bytes must be inaccessible during B read") }
    bytesB.complete(byteArrayOf(2))
    read.join()
    assertEquals("B", state.fileName)
    assertFalse(state.isReading)
    state.parse { bytes, _ ->
      assertArrayEquals(byteArrayOf(2), bytes)
      success
    }
    assertEquals(success, state.inspection)
  }

  @Test
  fun staleReadFailureAndCompletionCannotChangeNewReadBusyState() = runBlocking {
    for (fail in listOf(false, true)) {
      val state = KeystoreBrowserState()
      val a = state.select()
      val finishA = CompletableDeferred<Unit>()
      val readA = launch(start = CoroutineStart.UNDISPATCHED) {
        state.read(a, "A") {
          finishA.await()
          if (fail) error("old read failure")
          byteArrayOf(1)
        }
      }
      val b = state.select()
      finishA.complete(Unit)
      readA.join()
      assertTrue(state.isReading)
      assertNull(state.fileName)
      assertNull(state.error)
      assertFalse(state.canParse)
      state.read(b, "B") { byteArrayOf(2) }
      assertEquals("B", state.fileName)
    }
  }

  @Test
  fun staleParseCannotPublishOrClearNewParseBusyStateAndPasswordsAreWiped() = runBlocking {
    val state = KeystoreBrowserState()
    state.read(state.select(), "A") { byteArrayOf(1) }
    state.password = "synthetic-A"
    val enteredA = CompletableDeferred<Unit>()
    val enteredB = CompletableDeferred<Unit>()
    val finishA = CountDownLatch(1)
    val finishB = CountDownLatch(1)
    var charsA: CharArray? = null
    var charsB: CharArray? = null
    val parseA = launch {
      state.parse { _, chars ->
        charsA = chars
        enteredA.complete(Unit)
        check(finishA.await(10, TimeUnit.SECONDS))
        success
      }
    }
    enteredA.await()
    state.read(state.select(), "B") { byteArrayOf(2) }
    state.password = "synthetic-B"
    val parseB = launch {
      state.parse { _, chars ->
        charsB = chars
        enteredB.complete(Unit)
        check(finishB.await(10, TimeUnit.SECONDS))
        KeystoreInspection.Failure(KeystoreError.EmptyKeystore)
      }
    }
    try {
      enteredB.await()
      finishA.countDown()
      parseA.join()
      assertTrue(state.isParsing)
      assertNull(state.inspection)
      assertEquals("synthetic-B", state.password)
      assertTrue(charsA!!.all { it == '\u0000' })
    } finally {
      finishA.countDown()
      finishB.countDown()
    }
    parseB.join()
    assertFalse(state.isParsing)
    assertTrue(charsB!!.all { it == '\u0000' })
    assertEquals(KeystoreInspection.Failure(KeystoreError.EmptyKeystore), state.inspection)
  }

  @Test
  fun parseExceptionAndCancellationWipePasswordAndResetBusy() = runBlocking {
    for (cancel in listOf(false, true)) {
      val state = KeystoreBrowserState()
      state.read(state.select(), "test") { byteArrayOf(1) }
      state.password = "synthetic-only"
      var snapshot: CharArray? = null
      try {
        state.parse { _, chars ->
          snapshot = chars
          if (cancel) throw CancellationException("cancel")
          error("sensitive provider message")
        }
        assertFalse(cancel)
        assertFalse(state.inspection.toString().contains("sensitive provider message"))
      } catch (_: CancellationException) {
        assertTrue(cancel)
        assertNull(state.inspection)
      }
      assertFalse(state.isParsing)
      assertTrue(snapshot!!.all { it == '\u0000' })
      state.clear()
      assertEquals("", state.password)
      assertFalse(state.canParse)
    }
  }

  @Test
  fun cancelledReadPropagatesAndClearsOnlyItsOwnBusyState() = runBlocking {
    val state = KeystoreBrowserState()
    try {
      state.read(state.select(), "test") { throw CancellationException("cancel") }
      fail("cancellation must propagate")
    } catch (_: CancellationException) {
      assertFalse(state.isReading)
      assertNull(state.error)
      assertFalse(state.canParse)
    }
  }
}
