package ui.screen

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import signature.KeystoreError
import signature.KeystoreFileFormat
import signature.KeystoreInspection
import java.util.concurrent.atomic.AtomicReference

private val parseSuccess = KeystoreInspection.Success(KeystoreFileFormat.PKCS12, emptyList())

class KeystoreBrowserViewModelTest {

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /**
   * Inspect test double: records the arguments each call saw (keyed by the
   * first payload byte), lets tests gate and fail individual parses. The char
   * array is recorded by reference so tests can observe the VM wiping it.
   */
  private class RecordingInspect : suspend (ByteArray, CharArray) -> KeystoreInspection {
    val charsByKey = mutableMapOf<Byte, CharArray>()
    val bytesByKey = mutableMapOf<Byte, ByteArray>()
    val entries = mutableMapOf<Byte, CompletableDeferred<Unit>>()
    val gates = mutableMapOf<Byte, CompletableDeferred<Unit>>()
    val results = mutableMapOf<Byte, KeystoreInspection>()

    override suspend fun invoke(bytes: ByteArray, chars: CharArray): KeystoreInspection {
      val key = bytes[0]
      bytesByKey[key] = bytes.copyOf()
      charsByKey[key] = chars
      entries.getOrPut(key) { CompletableDeferred() }.complete(Unit)
      gates[key]?.await()
      return results.getOrDefault(key, parseSuccess)
    }
  }

  /** Registers the ViewModel with a production [ViewModelStore] owner. */
  private fun createdInStore(
    inspect: suspend (ByteArray, CharArray) -> KeystoreInspection,
  ): Pair<KeystoreBrowserViewModel, ViewModelStore> {
    val store = ViewModelStore()
    val viewModel = KeystoreBrowserViewModel(inspect)
    store.put("browser", viewModel)
    return viewModel to store
  }

  @Test
  fun delayedBReadCannotParseABytesWithBSelection() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val inspect = RecordingInspect()
    val (viewModel, _) = createdInStore(inspect)

    viewModel.onFileSelected("A") { byteArrayOf(1) }
    advanceUntilIdle()
    viewModel.onPasswordChanged("synthetic-password")
    viewModel.parse()
    inspect.entries.getValue(1).await()
    advanceUntilIdle()
    assertEquals(parseSuccess, viewModel.inspection)
    assertEquals("", viewModel.password) // a successful parse clears the password

    viewModel.onPasswordChanged("another-synthetic-password")
    val bytesB = CompletableDeferred<ByteArray>()
    viewModel.onFileSelected("B") { bytesB.await() }
    // The picker-callback reset is synchronous inside onFileSelected.
    assertNull(viewModel.fileName)
    assertNull(viewModel.inspection)
    assertEquals("", viewModel.password)
    assertTrue(viewModel.isReading)
    assertFalse(viewModel.canParse)

    viewModel.parse() // must be rejected: no bytes are loaded for B yet
    advanceUntilIdle()
    assertNull(viewModel.inspection)
    assertFalse(viewModel.isParsing)

    bytesB.complete(byteArrayOf(2))
    advanceUntilIdle()
    assertEquals("B", viewModel.fileName)
    assertFalse(viewModel.isReading)

    inspect.entries[2] = CompletableDeferred()
    viewModel.parse()
    inspect.entries.getValue(2).await()
    advanceUntilIdle()
    assertArrayEquals(byteArrayOf(2), inspect.bytesByKey.getValue(2))
    assertEquals(parseSuccess, viewModel.inspection)
  }

  @Test
  fun staleReadFailureAndCompletionCannotChangeNewReadBusyState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    for (fail in listOf(false, true)) {
      val (viewModel, _) = createdInStore { _, _ -> parseSuccess }
      val finishA = CompletableDeferred<Unit>()
      viewModel.onFileSelected("A") {
        finishA.await()
        if (fail) error("old read failure")
        byteArrayOf(1)
      }
      val bytesB = CompletableDeferred<ByteArray>()
      viewModel.onFileSelected("B") { bytesB.await() }
      finishA.complete(Unit)
      advanceUntilIdle()
      // The stale A read finished (or failed) but must not touch B's state.
      assertTrue(viewModel.isReading)
      assertNull(viewModel.fileName)
      assertNull(viewModel.error)
      assertFalse(viewModel.canParse)

      bytesB.complete(byteArrayOf(2))
      advanceUntilIdle()
      assertEquals("B", viewModel.fileName)
      assertFalse(viewModel.isReading)
    }
  }

  @Test
  fun staleParseCannotPublishOrClearNewParseStateAndPasswordsAreWiped() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val inspect = RecordingInspect()
    val (viewModel, _) = createdInStore(inspect)

    // Warm-up: A parses successfully once.
    viewModel.onFileSelected("A") { byteArrayOf(1) }
    advanceUntilIdle()
    viewModel.onPasswordChanged("synthetic-password")
    viewModel.parse()
    inspect.entries.getValue(1).await()
    advanceUntilIdle()
    assertEquals(parseSuccess, viewModel.inspection)

    // Re-select A and start a fresh gated parse for it.
    inspect.entries[1] = CompletableDeferred()
    inspect.gates[1] = CompletableDeferred()
    inspect.results[1] = KeystoreInspection.Failure(KeystoreError.EmptyKeystore)
    viewModel.onFileSelected("A") { byteArrayOf(1) }
    advanceUntilIdle()
    viewModel.onPasswordChanged("synthetic-A")
    viewModel.parse()
    inspect.entries.getValue(1).await()
    assertTrue(viewModel.isParsing)

    // A newer selection resets the state; then B's own gated parse starts.
    val bytesB = CompletableDeferred<ByteArray>()
    viewModel.onFileSelected("B") { bytesB.await() }
    bytesB.complete(byteArrayOf(2))
    advanceUntilIdle()
    viewModel.onPasswordChanged("synthetic-B")
    inspect.entries[2] = CompletableDeferred()
    inspect.gates[2] = CompletableDeferred()
    inspect.results[2] = KeystoreInspection.Failure(KeystoreError.EmptyKeystore)
    viewModel.parse()
    inspect.entries.getValue(2).await()
    assertTrue(viewModel.isParsing)

    // Let the stale A parse finish while B is still gated.
    inspect.gates.getValue(1).complete(Unit)
    advanceUntilIdle()
    assertNull(viewModel.inspection) // stale result must never publish
    assertEquals("synthetic-B", viewModel.password) // nor clear the new password
    assertTrue(inspect.charsByKey.getValue(1).all { it == '\u0000' })
    assertTrue(viewModel.isParsing) // B's parse state is untouched by A

    // B's own parse finishes: its result publishes, its copy is wiped.
    inspect.gates.getValue(2).complete(Unit)
    advanceUntilIdle()
    assertTrue(inspect.charsByKey.getValue(2).all { it == '\u0000' })
    assertEquals(
      KeystoreInspection.Failure(KeystoreError.EmptyKeystore),
      viewModel.inspection,
    )
    assertEquals("synthetic-B", viewModel.password) // only success clears it
    assertFalse(viewModel.isParsing)
  }

  @Test
  fun parseExceptionAndCancellationWipePasswordCopyAndResetBusy() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    for (cancel in listOf(false, true)) {
      val snapshot = AtomicReference<CharArray?>(null)
      val throwingInspect: suspend (ByteArray, CharArray) -> KeystoreInspection = { _, chars ->
        snapshot.set(chars)
        if (cancel) throw CancellationException("cancel")
        error("sensitive provider message")
      }
      val (viewModel, _) = createdInStore(throwingInspect)
      viewModel.onFileSelected("test") { byteArrayOf(1) }
      advanceUntilIdle()
      viewModel.onPasswordChanged("synthetic-only")
      viewModel.parse()
      advanceUntilIdle()

      assertFalse(viewModel.isParsing)
      if (cancel) {
        assertNull(viewModel.inspection)
      } else {
        assertEquals(
          KeystoreInspection.Failure(KeystoreError.ParseFailed("Provider could not read this keystore")),
          viewModel.inspection,
        )
        assertFalse(viewModel.inspection.toString().contains("sensitive provider message"))
      }
      assertTrue(snapshot.get()!!.all { it == '\u0000' })
    }
  }

  @Test
  fun cancelledReadLeavesNoBusyStateAndNoError() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val (viewModel, _) = createdInStore { _, _ -> parseSuccess }
    viewModel.onFileSelected("test") { throw CancellationException("cancel") }
    advanceUntilIdle()
    assertFalse(viewModel.isReading)
    assertNull(viewModel.error)
    assertFalse(viewModel.canParse)
  }

  @Test
  fun failedReadPublishesErrorForTheCurrentSelectionOnly() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val (viewModel, _) = createdInStore { _, _ -> parseSuccess }
    viewModel.onFileSelected("test") { error("Could not open the selected file") }
    advanceUntilIdle()
    assertEquals("Could not open the selected file", viewModel.error)
    assertFalse(viewModel.isReading)
    assertFalse(viewModel.canParse)

    // A later successful selection clears the stale error.
    viewModel.onFileSelected("good") { byteArrayOf(1) }
    advanceUntilIdle()
    assertNull(viewModel.error)
    assertTrue(viewModel.canParse)
  }

  @Test
  fun ownerStoreClearWipesSecretsAndBlocksLateParsePublication() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val inspect = RecordingInspect()
    val (viewModel, store) = createdInStore(inspect)
    inspect.entries[1] = CompletableDeferred()
    inspect.gates[1] = CompletableDeferred()
    viewModel.onFileSelected("A") { byteArrayOf(1) }
    advanceUntilIdle()
    viewModel.onPasswordChanged("synthetic-password")
    viewModel.parse()
    inspect.entries.getValue(1).await()
    assertTrue(viewModel.isParsing)

    // The production owner clear (entry pop): cancels the ViewModel scope and
    // runs onCleared, which must wipe password/bytes and invalidate the
    // selection so the gated parse can never publish.
    store.clear()
    assertEquals("", viewModel.password)
    assertNull(viewModel.fileName)
    assertNull(viewModel.inspection)
    assertFalse(viewModel.canParse)
    assertFalse(viewModel.isReading)
    assertFalse(viewModel.isParsing)

    inspect.gates.getValue(1).complete(Unit)
    advanceUntilIdle()
    assertNull(viewModel.inspection)
    assertTrue(inspect.charsByKey.getValue(1).all { it == '\u0000' })
  }
}
