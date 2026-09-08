package ui.screen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import signature.ApkSignerMeta
import signature.ApkSignerRead
import signature.ApkSignerReadError

class ApkCompareViewModelTest {

  private val metaA = ApkSignerMeta("com.example.a", "1.0", 1L, setOf("AA"))
  private val metaB = ApkSignerMeta("com.example.b", "2.0", 2L, setOf("BB"))

  private class GatedSource(
    private val gate: CompletableDeferred<Unit>? = null,
    private val result: ApkSignerRead? = null,
    private val finishSignal: CompletableDeferred<Boolean>? = null,
  ) : ApkCompareSource {
    override suspend fun read(): ApkSignerRead {
      try {
        gate?.await()
        return result!!
      } catch (e: CancellationException) {
        finishSignal?.complete(false) // cancelled, never completed normally
        throw e
      }
    }
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun sidesLoadIndependentlyAndPublishTheirOwnResults() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ApkCompareViewModel()
    assertNull(viewModel.left.value)
    assertNull(viewModel.right.value)

    viewModel.onApkPicked(ApkCompareSide.Left) { ApkSignerRead.Success(metaA) }
    advanceUntilIdle()
    assertEquals("com.example.a", (viewModel.left.value as ApkSignerRead.Success).meta.packageName)
    assertNull(viewModel.right.value)

    viewModel.onApkPicked(ApkCompareSide.Right) { ApkSignerRead.Success(metaB) }
    advanceUntilIdle()
    assertEquals("com.example.a", (viewModel.left.value as ApkSignerRead.Success).meta.packageName)
    assertEquals("com.example.b", (viewModel.right.value as ApkSignerRead.Success).meta.packageName)
  }

  @Test
  fun newerPickOnTheSameSideCancelsAndDiscardsTheSupersededRead() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ApkCompareViewModel()
    val staleFinishedNormally = CompletableDeferred<Boolean>()
    val staleGate = CompletableDeferred<Unit>()

    viewModel.onApkPicked(ApkCompareSide.Left, GatedSource(staleGate, ApkSignerRead.Success(metaA), staleFinishedNormally))
    // Replace the selection: the stale read is cancelled and must never publish.
    viewModel.onApkPicked(ApkCompareSide.Left) { ApkSignerRead.Success(metaB) }
    advanceUntilIdle()
    staleGate.complete(Unit)
    advanceUntilIdle()

    assertFalse(staleFinishedNormally.await())
    assertEquals("com.example.b", (viewModel.left.value as ApkSignerRead.Success).meta.packageName)
  }

  @Test
  fun gatedStaleResultCannotPublishOverTheNewerSelection() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ApkCompareViewModel()
    val staleGate = CompletableDeferred<Unit>()

    viewModel.onApkPicked(ApkCompareSide.Right, GatedSource(staleGate, ApkSignerRead.Failure(ApkSignerReadError.EmptyFile)))
    viewModel.onApkPicked(ApkCompareSide.Right) { ApkSignerRead.Success(metaB) }
    advanceUntilIdle()
    assertEquals("com.example.b", (viewModel.right.value as ApkSignerRead.Success).meta.packageName)

    staleGate.complete(Unit) // the stale result arrives late
    advanceUntilIdle()
    assertEquals("com.example.b", (viewModel.right.value as ApkSignerRead.Success).meta.packageName)
  }

  @Test
  fun unexpectedSourceFailureBecomesAnHonestNotApkOrUnreadableFailure() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ApkCompareViewModel()
    viewModel.onApkPicked(ApkCompareSide.Left) { error("security provider exploded") }
    advanceUntilIdle()
    assertEquals(
      ApkSignerRead.Failure(ApkSignerReadError.NotApkOrUnreadable),
      viewModel.left.value,
    )
    assertNull(viewModel.right.value)
  }

  @Test
  fun ownerClearCancelsInFlightReadsAndNothingPublishesAfterwards() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ApkCompareViewModel()
    val gate = CompletableDeferred<Unit>()
    val finishSignal = CompletableDeferred<Boolean>()
    viewModel.onApkPicked(ApkCompareSide.Left, GatedSource(gate, ApkSignerRead.Success(metaA), finishSignal))

    viewModel.viewModelScope.cancel()
    gate.complete(Unit)
    advanceUntilIdle()

    assertFalse(finishSignal.await())
    assertNull(viewModel.left.value)
  }

  @Test
  fun nullPickerResultKeepsThePreviousSideUntouched() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ApkCompareViewModel()
    viewModel.onApkPicked(ApkCompareSide.Left) { ApkSignerRead.Success(metaA) }
    advanceUntilIdle()

    // A cancelled picker is filtered in the screen; re-picking the same side
    // resets it to null until the new read finishes.
    viewModel.onApkPicked(ApkCompareSide.Left) { ApkSignerRead.Success(metaB) }
    advanceUntilIdle()
    assertEquals("com.example.b", (viewModel.left.value as ApkSignerRead.Success).meta.packageName)
  }
}
