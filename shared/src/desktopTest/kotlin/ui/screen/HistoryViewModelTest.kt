package ui.screen

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryViewModelTest {

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun entriesMirrorRepositoryFlow() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeHistoryRepository()
    val viewModel = HistoryViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.entries.collect { /* keep whileSubscribed active like collectAsState */ }
    }

    assertEquals(emptyList<String>(), viewModel.entries.value.map { it.packageName })

    repository.record("com.example.notes", "Notes")
    repository.record("com.example.reader", "Reader")
    assertEquals(
      listOf("com.example.reader", "com.example.notes"),
      viewModel.entries.value.map { it.packageName },
    )
  }

  @Test
  fun removeDelegatesToRepository() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeHistoryRepository()
    repository.record("com.example.notes", "Notes")
    val viewModel = HistoryViewModel(repository)

    viewModel.remove("com.example.notes")
    advanceUntilIdle()

    assertEquals(listOf("com.example.notes"), repository.removed)
    assertEquals(emptyList<String>(), viewModel.entries.value.map { it.packageName })
  }

  @Test
  fun clearDelegatesToRepository() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeHistoryRepository()
    repository.record("com.example.notes", "Notes")
    val viewModel = HistoryViewModel(repository)

    viewModel.clear()
    advanceUntilIdle()

    assertEquals(1, repository.clearCount)
    assertEquals(emptyList<String>(), viewModel.entries.value.map { it.packageName })
  }

  @Test
  fun removePersistsEvenIfEntryScopedScopeIsCancelledMidWrite() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeHistoryRepository()
    repository.record("com.example.notes", "Notes")
    repository.record("com.example.reader", "Reader")
    val gate = CompletableDeferred<Unit>()

    val viewModel = HistoryViewModel(repository)
    // Keep the whileSubscribed collection active across the gated write.
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.entries.collect { /* keep whileSubscribed active */ }
    }
    repository.gate = gate

    viewModel.remove("com.example.notes")
    viewModel.viewModelScope.cancel()
    gate.complete(Unit)
    advanceUntilIdle()

    assertEquals(listOf("com.example.notes"), repository.removed)
  }
}
