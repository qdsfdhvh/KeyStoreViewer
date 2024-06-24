package ui.component.voyager

import androidx.compose.runtime.Composable
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.StateFlow

abstract class MoleculeScreenModel<T> : ScreenModel {

  val state: StateFlow<T> by lazy {
    screenModelScope.launchMolecule(
      mode = RecompositionMode.ContextClock,
      context = AndroidUiDispatcher.Main,
      body = { present() },
    )
  }

  @Composable
  protected abstract fun present(): T
}
