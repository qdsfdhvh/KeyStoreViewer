package ui.screen

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewModelScope
import data.model.UiAppInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ui.component.state.UiState

class AppListViewModelTest {

  private val userApp = UiAppInfo("com.example.notes", "Notes", 1L, "1.0", 10L, null)
  private val otherUserApp = UiAppInfo("com.example.reader", "Reader", 3L, "3.0", 5L, null)
  private val systemApp = UiAppInfo("com.android.settings", "Settings", 2L, "2.0", 20L, null)

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private class FakeAppListSource : AppListSource {
    val loadCounts = mutableMapOf<AppType, Int>()
    val gates = mutableMapOf<AppType, CompletableDeferred<Unit>>()
    var appsByType: Map<AppType, List<UiAppInfo>> = mapOf(
      AppType.User to listOf<UiAppInfo>(),
      AppType.System to listOf<UiAppInfo>(),
    )

    fun given(type: AppType, apps: List<UiAppInfo>) {
      appsByType = appsByType + (type to apps)
    }

    override suspend fun load(type: AppType): List<UiAppInfo> {
      loadCounts[type] = (loadCounts[type] ?: 0) + 1
      gates[type]?.await()
      return appsByType[type].orEmpty()
    }
  }

  @Test
  fun initLoadsUserAppsAndRefreshReloadsFromSource() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val source = FakeAppListSource().apply {
      given(AppType.User, listOf(userApp, otherUserApp))
      given(AppType.System, listOf(systemApp))
    }
    val viewModel = AppListViewModel(source, FakeHistoryRepository(), FakeFavoritesRepository())
    val states = mutableListOf<AppListScreenState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { states += it }
    }

    assertEquals(AppType.User, states.last().appType)
    assertEquals(
      listOf("com.example.notes", "com.example.reader"),
      loadedPackageNames(states.last().displayPackages),
    )

    viewModel.onEvent(AppListScreenEvent.Refresh)
    advanceUntilIdle()

    assertEquals(2, source.loadCounts[AppType.User])
    assertEquals(
      listOf("com.example.notes", "com.example.reader"),
      loadedPackageNames(states.last().displayPackages),
    )
  }

  @Test
  fun queryFiltersLoadedAppsByPackageOrDisplayNameIgnoringCase() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val source = FakeAppListSource().apply {
      given(AppType.User, listOf(userApp, otherUserApp))
    }
    val viewModel = AppListViewModel(source, FakeHistoryRepository(), FakeFavoritesRepository())
    val states = mutableListOf<AppListScreenState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { states += it }
    }

    viewModel.onEvent(AppListScreenEvent.OnQueryChanged(TextFieldValue("READER")))
    advanceUntilIdle()

    assertEquals(
      listOf("com.example.reader"),
      loadedPackageNames(states.last().displayPackages),
    )

    viewModel.onEvent(AppListScreenEvent.OnQueryChanged(TextFieldValue("com.example.notes")))
    advanceUntilIdle()

    assertEquals(
      listOf("com.example.notes"),
      loadedPackageNames(states.last().displayPackages),
    )
  }

  @Test
  fun appTypeSwitchReloadsForNewTypeAndKeepsSameTypeFreeOfReloads() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val source = FakeAppListSource().apply {
      given(AppType.User, listOf(userApp))
      given(AppType.System, listOf(systemApp))
    }
    val viewModel = AppListViewModel(source, FakeHistoryRepository(), FakeFavoritesRepository())
    val states = mutableListOf<AppListScreenState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { states += it }
    }
    assertEquals(1, source.loadCounts[AppType.User])

    viewModel.onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.System))
    advanceUntilIdle()
    assertEquals(AppType.System, states.last().appType)
    assertEquals(listOf("com.android.settings"), loadedPackageNames(states.last().displayPackages))
    assertEquals(1, source.loadCounts[AppType.System])

    viewModel.onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.System))
    advanceUntilIdle()
    assertEquals(1, source.loadCounts[AppType.System])

    viewModel.onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.User))
    advanceUntilIdle()
    assertEquals(2, source.loadCounts[AppType.User])
  }

  @Test
  fun staleLoadOfCancelledTypeCannotPublishItsResult() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val source = FakeAppListSource().apply {
      given(AppType.User, listOf(userApp))
      given(AppType.System, listOf(systemApp))
      gates[AppType.User] = CompletableDeferred()
    }
    val viewModel = AppListViewModel(source, FakeHistoryRepository(), FakeFavoritesRepository())
    val states = mutableListOf<AppListScreenState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.state.collect { states += it }
    }
    assertTrue(states.last().displayPackages is UiState.Loading)

    viewModel.onEvent(AppListScreenEvent.OnAppTypeChanged(AppType.System))
    advanceUntilIdle()
    assertEquals(listOf("com.android.settings"), loadedPackageNames(states.last().displayPackages))

    // The abandoned user load must never overwrite the newer system result.
    source.gates.getValue(AppType.User).complete(Unit)
    advanceUntilIdle()
    assertEquals(listOf("com.android.settings"), loadedPackageNames(states.last().displayPackages))
  }

  @Test
  fun recordViewedPersistsEvenIfEntryScopedScopeIsCancelledMidWrite() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val history = FakeHistoryRepository()
    val gate = CompletableDeferred<Unit>()
    history.gate = gate
    val viewModel = AppListViewModel(FakeAppListSource(), history, FakeFavoritesRepository())

    viewModel.recordViewed(userApp)
    viewModel.viewModelScope.cancel()
    gate.complete(Unit)
    advanceUntilIdle()

    assertEquals(listOf("com.example.notes"), history.recorded)
  }

  @Test
  fun toggleFavoritePersistsEvenIfEntryScopedScopeIsCancelledMidWrite() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val favorites = FakeFavoritesRepository()
    val gate = CompletableDeferred<Unit>()
    favorites.gate = gate
    val viewModel = AppListViewModel(FakeAppListSource(), FakeHistoryRepository(), favorites)

    viewModel.toggleFavorite(userApp)
    viewModel.viewModelScope.cancel()
    gate.complete(Unit)
    advanceUntilIdle()

    assertEquals(listOf("com.example.notes"), favorites.toggled)
  }

  @Test
  fun filterAppsMatchesPackageOrNameIgnoringCaseAndKeepsLoadingUntouched() {
    val apps = listOf(userApp, systemApp, otherUserApp)

    assertEquals(
      listOf("com.example.reader"),
      loadedPackageNames(filterApps(UiState.Loaded(apps), "READER")),
    )
    assertEquals(
      listOf("com.android.settings"),
      loadedPackageNames(filterApps(UiState.Loaded(apps), "settings")),
    )
    assertEquals(
      listOf("com.example.notes", "com.android.settings", "com.example.reader"),
      loadedPackageNames(filterApps(UiState.Loaded(apps), "")),
    )
    assertEquals(UiState.Loading, filterApps(UiState.Loading, "anything"))
    assertNull(loadedPackageNames(UiState.Loading))
  }

  private fun loadedPackageNames(state: UiState<List<UiAppInfo>>): List<String>? = when (state) {
    UiState.Loading -> null
    is UiState.Loaded -> state.data.map { it.packageName }
  }
}
