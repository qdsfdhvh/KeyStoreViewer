package ui.component

import androidx.compose.runtime.Composable
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

abstract class MoleculeScreenModel<T> {

  private val scope = CoroutineScope(SupervisorJob() + AndroidUiDispatcher.Main)

  val state: StateFlow<T> by lazy {
    scope.launchMolecule(
      mode = RecompositionMode.ContextClock,
      context = AndroidUiDispatcher.Main,
      body = { present() },
    )
  }

  @Composable
  protected abstract fun present(): T

  fun dispose() {
    scope.cancel()
  }
}
