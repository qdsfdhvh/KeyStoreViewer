package ui.screen

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.local.FavoritesRepository
import data.local.HistoryRepository
import data.model.UiAppInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.component.state.UiState

/** Loads the installed app list for one [AppType]; implementations must be main-safe. */
fun interface AppListSource {
  suspend fun load(type: AppType): List<UiAppInfo>
}

class AppListViewModel(
  private val source: AppListSource,
  private val historyRepository: HistoryRepository,
  private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

  private val query = MutableStateFlow(TextFieldValue())
  private val appType = MutableStateFlow(AppType.User)
  private val packages = MutableStateFlow<UiState<List<UiAppInfo>>>(UiState.Loading)

  private var loadJob: Job? = null

  init {
    load()
  }

  val state: StateFlow<AppListScreenState> = combine(
    query,
    appType,
    packages,
  ) { query, appType, packages ->
    AppListScreenState(
      query = query,
      appType = appType,
      displayPackages = filterApps(packages, query.text),
      eventSink = ::onEvent,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = AppListScreenState(TextFieldValue(), AppType.User, UiState.Loading, ::onEvent),
  )

  fun onEvent(event: AppListScreenEvent) {
    when (event) {
      AppListScreenEvent.Refresh -> load()

      is AppListScreenEvent.OnQueryChanged -> query.value = event.query

      is AppListScreenEvent.OnAppTypeChanged -> {
        if (appType.value != event.type) {
          appType.value = event.type
          load()
        }
      }
    }
  }

  /** Persists the visit even if the entry-scoped store clears this VM mid-write. */
  fun recordViewed(app: UiAppInfo) {
    viewModelScope.launch {
      withContext(NonCancellable) {
        historyRepository.record(app.packageName, app.name)
      }
    }
  }

  /** Toggles the favorite even if the entry-scoped store clears this VM mid-write. */
  fun toggleFavorite(app: UiAppInfo) {
    viewModelScope.launch {
      withContext(NonCancellable) {
        favoritesRepository.toggle(app.packageName, app.name)
      }
    }
  }

  private fun load() {
    loadJob?.cancel()
    val type = appType.value
    packages.value = UiState.Loading
    loadJob = viewModelScope.launch {
      val loaded = source.load(type).sortedByDescending { it.lastUpdateTime }
      packages.update { UiState.Loaded(loaded) }
    }
  }
}

internal fun filterApps(
  packages: UiState<List<UiAppInfo>>,
  query: String,
): UiState<List<UiAppInfo>> = when (packages) {
  UiState.Loading -> UiState.Loading

  is UiState.Loaded -> UiState.Loaded(
    packages.data.filter {
      it.packageName.contains(query, ignoreCase = true) ||
        it.name.contains(query, ignoreCase = true)
    },
  )
}

data class AppListScreenState(
  val query: TextFieldValue,
  val appType: AppType,
  val displayPackages: UiState<List<UiAppInfo>>,
  val eventSink: (AppListScreenEvent) -> Unit,
)

enum class AppType {
  User,
  System,
}

sealed interface AppListScreenEvent {
  data object Refresh : AppListScreenEvent

  data class OnQueryChanged(val query: TextFieldValue) : AppListScreenEvent

  data class OnAppTypeChanged(val type: AppType) : AppListScreenEvent
}
