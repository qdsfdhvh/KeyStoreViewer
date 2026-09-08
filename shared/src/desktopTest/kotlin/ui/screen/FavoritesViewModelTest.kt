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

class FavoritesViewModelTest {

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun entriesMirrorRepositoryFlow() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeFavoritesRepository()
    val viewModel = FavoritesViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.entries.collect { /* keep whileSubscribed active like collectAsState */ }
    }

    assertEquals(emptyList<String>(), viewModel.entries.value.map { it.packageName })

    repository.toggle("com.example.notes", "Notes")
    assertEquals(
      listOf("com.example.notes"),
      viewModel.entries.value.map { it.packageName },
    )
  }

  @Test
  fun removeDelegatesToRepository() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeFavoritesRepository()
    repository.toggle("com.example.notes", "Notes")
    val viewModel = FavoritesViewModel(repository)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.entries.collect { /* keep whileSubscribed active like collectAsState */ }
    }

    viewModel.remove("com.example.notes")
    advanceUntilIdle()

    assertEquals(listOf("com.example.notes"), repository.removed)
    assertEquals(emptyList<String>(), viewModel.entries.value.map { it.packageName })
  }

  @Test
  fun removePersistsEvenIfEntryScopedScopeIsCancelledMidWrite() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val repository = FakeFavoritesRepository()
    repository.toggle("com.example.notes", "Notes")
    repository.toggle("com.example.reader", "Reader")
    val gate = CompletableDeferred<Unit>()

    val viewModel = FavoritesViewModel(repository)
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
