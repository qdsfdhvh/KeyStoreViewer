package ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.local.FavoriteEntry
import data.local.FavoritesRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
  private val repository: FavoritesRepository,
) : ViewModel() {

  val entries: StateFlow<List<FavoriteEntry>> = repository.entries.stateIn(
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
}
