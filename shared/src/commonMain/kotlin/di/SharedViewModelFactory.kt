package di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * App-scoped [MetroViewModelFactory]: Metro aggregates every `@ViewModelKey`
 * (and assisted-factory) map contribution on the classpath into the three
 * provider maps. The androidApp graph binds this instance as
 * [dev.zacsweers.metrox.viewmodel.ViewModelGraph.metroViewModelFactory] and
 * MainActivity exposes it via LocalMetroViewModelFactory; ViewModels built
 * through it stay scoped to their navigation entries, never to the app.
 */
@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class SharedViewModelFactory(
  override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
  override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
  override val manualAssistedFactoryProviders: Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
