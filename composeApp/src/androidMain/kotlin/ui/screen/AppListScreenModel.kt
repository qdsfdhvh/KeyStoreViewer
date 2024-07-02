package ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.input.TextFieldValue
import data.model.AppInfoEntry
import data.model.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ui.component.state.UiState
import ui.component.voyager.MoleculeScreenModel
import util.getSystemAppInfos
import util.getUserInstalledAppInfos

class AppListScreenModel(
  private val applicationContext: Context,
) : MoleculeScreenModel<AppListScreenState>() {
  @Composable
  override fun present(): AppListScreenState {
    var query by remember { mutableStateOf(TextFieldValue("")) }

    var refreshKey by remember { mutableIntStateOf(0) }
    var selectAppType by remember { mutableStateOf(AppType.User) }
    val packages by produceState<UiState<List<AppInfoEntry>>>(UiState.Loading, selectAppType, refreshKey) {
      value = UiState.Loading
      value = withContext(Dispatchers.IO) {
        UiState.Loaded(
          when (selectAppType) {
            AppType.User -> applicationContext.getUserInstalledAppInfos()
            AppType.System -> applicationContext.getSystemAppInfos()
          }.map {
            AppInfoEntry.from(it, applicationContext)
          }.sortedByDescending {
            it.lastUpdateTime
          },
        )
      }
    }
    val displayPackages by remember {
      combine(
        snapshotFlow { query.text },
        snapshotFlow { packages },
      ) { query, packages ->
        when (packages) {
          UiState.Loading -> UiState.Loading
          is UiState.Loaded -> {
            UiState.Loaded(
              packages.data.filter {
                it.packageName.contains(query, ignoreCase = true) ||
                  it.name.contains(query, ignoreCase = true)
              },
            )
          }
        }
      }.flowOn(Dispatchers.IO)
    }.collectAsState(UiState.Loading)
    return AppListScreenState(
      query = query,
      appType = selectAppType,
      displayPackages = displayPackages,
      eventSink = { event ->
        when (event) {
          AppListScreenEvent.Refresh -> {
            refreshKey++
          }

          is AppListScreenEvent.OnQueryChanged -> {
            query = event.query
          }

          is AppListScreenEvent.OnAppTypeChanged -> {
            selectAppType = event.type
          }

          else -> Unit
        }
      },
    )
  }
}

data class AppListScreenState(
  val query: TextFieldValue,
  val appType: AppType,
  val displayPackages: UiState<List<AppInfoEntry>>,
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
  data class OnItemClick(val packageName: String) : AppListScreenEvent
}
