package ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.local.HistoryEntry
import data.local.HistoryRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryViewModel(
  private val repository: HistoryRepository,
) : ViewModel() {

  val entries: StateFlow<List<HistoryEntry>> = repository.entries.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = emptyList(),
  )

  /** Completes the removal even if the entry-scoped store clears this VM mid-write. */
  fun remove(packageName: String) {
    viewModelScope.launch {
      withContext(NonCancellable) {
        repository.remove(packageName)
      }
    }
  }

  /** Completes the clear even if the entry-scoped store clears this VM mid-write. */
  fun clear() {
    viewModelScope.launch {
      withContext(NonCancellable) {
        repository.clear()
      }
    }
  }
}
