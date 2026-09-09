package ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.model.SignSource
import data.model.UiAppInfo
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okio.ByteString

/** Loads [SignatureDetailData] for a [SignSource]; implementations must be main-safe. */
fun interface SignatureDetailLoader {
  /** null mirrors the previous behavior of an unresolvable package/archive: no content. */
  suspend fun load(signSource: SignSource): SignatureDetailData?
}

class SignatureDetailData(
  val appInfo: UiAppInfo,
  val signatures: List<SignatureDetailCertificate>,
)

/** One signing certificate with its precomputed public-key modulus representations. */
class SignatureDetailCertificate(
  val bytes: ByteString,
  val modulusHex: String,
  val modulusString: String,
)

/**
 * Per-entry detail ViewModel. The runtime [signSource] is an assisted
 * argument: each navigation entry creates its own instance through
 * [Factory], while the loader binding comes from the app graph.
 */
class SignatureDetailViewModel @AssistedInject constructor(
  @Assisted val signSource: SignSource,
  private val loader: SignatureDetailLoader,
) : ViewModel() {

  private val _data = MutableStateFlow<SignatureDetailData?>(null)

  /** null while loading (or when the source cannot be resolved): the shell renders nothing. */
  val data: StateFlow<SignatureDetailData?> = _data.asStateFlow()

  init {
    viewModelScope.launch {
      _data.value = loader.load(signSource)
    }
  }

  /** Manual assisted factory: the screen supplies [signSource] at creation. */
  @AssistedFactory
  @ContributesIntoMap(AppScope::class)
  @ManualViewModelAssistedFactoryKey(Factory::class)
  fun interface Factory : ManualViewModelAssistedFactory {
    fun create(signSource: SignSource): SignatureDetailViewModel
  }
}
