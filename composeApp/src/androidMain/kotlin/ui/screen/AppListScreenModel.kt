package ui.screen

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import data.model.AppInfoEntry
import data.model.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import ui.component.state.UiState
import ui.component.voyager.MoleculeScreenModel
import util.getAppInfos

class AppListScreenModel(
  private val applicationContext: Context,
) : MoleculeScreenModel<AppListScreenState>() {
  @Composable
  override fun present(): AppListScreenState {
    var query by rememberSaveable { mutableStateOf("") }
    val packages by produceState<UiState<List<AppInfoEntry>>>(UiState.Loading) {
      value = UiState.Loading
      value = withContext(Dispatchers.IO) {
        UiState.Loaded(
          applicationContext.getAppInfos().map {
            AppInfoEntry.from(it, applicationContext)
          },
        )
      }
    }
    val displayPackages by remember {
      combine(
        snapshotFlow { query },
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
      displayPackages = displayPackages,
      eventSink = { event ->
        when (event) {
          is AppListScreenEvent.OnQueryChanged -> {
            query = event.query
          }
          else -> Unit
        }
      },
    )
  }
}

data class AppListScreenState(
  val query: String,
  val displayPackages: UiState<List<AppInfoEntry>>,
  val eventSink: (AppListScreenEvent) -> Unit,
)

sealed interface AppListScreenEvent {
  data class OnQueryChanged(val query: String) : AppListScreenEvent
  data class OnItemClick(val packageName: String) : AppListScreenEvent
}
