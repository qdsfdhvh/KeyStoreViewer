package ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import signature.KeystoreCrypto
import signature.KeystoreError
import signature.KeystoreInspection
import signature.KeystoreInspector
import signature.SelectionGeneration

/**
 * Lifecycle-owned state of the read-only KeyStore file browser.
 *
 * The password and the raw keystore bytes live only in this ViewModel's
 * memory: never persisted, never logged. They are dropped and overwritten as
 * soon as the navigation entry is popped ([onCleared]), and a new file
 * selection replaces them. A late parse/read from a superseded selection can
 * never publish its result ([SelectionGeneration]).
 *
 * [inspect] must be main-safe; the default dispatches to [Dispatchers.IO].
 */
class KeystoreBrowserViewModel(
  private val inspect: suspend (ByteArray, CharArray) -> KeystoreInspection = { bytes, chars ->
    withContext(Dispatchers.IO) {
      KeystoreInspector.parse(bytes, chars, KeystoreCrypto::providerFor)
    }
  },
) : ViewModel() {

  private val generation = SelectionGeneration()
  private data class Loaded(val token: Int, val bytes: ByteArray)
  private var loaded: Loaded? = null

  var fileName by mutableStateOf<String?>(null)
    private set
  var password by mutableStateOf("")
    private set
  var inspection by mutableStateOf<KeystoreInspection?>(null)
    private set
  var error by mutableStateOf<String?>(null)
    private set
  var isReading by mutableStateOf(false)
    private set
  var isParsing by mutableStateOf(false)
    private set
  var canParse by mutableStateOf(false)
    private set

  fun onPasswordChanged(value: String) {
    password = value
  }

  /**
   * Accepts the file picked in the system document picker. The reset runs
   * synchronously in the picker callback; the read continues on the ViewModel
   * scope and survives configuration changes. [readBytes] must be main-safe
   * and may hold no Activity reference (application context only).
   */
  fun onFileSelected(fileName: String?, readBytes: suspend () -> ByteArray) {
    val token = startSelection()
    viewModelScope.launch {
      read(token, fileName, readBytes)
    }
  }

  /** Parses the loaded keystore on the ViewModel scope. */
  fun parse() {
    val selection = loaded ?: return
    if (!canParse || isReading || isParsing || !generation.isValid(selection.token)) return
    val secret = password
    isParsing = true
    viewModelScope.launch {
      // Allocate synchronously so no cancellation window can skip the wipe,
      // and never log provider exceptions: their messages may include
      // sensitive input.
      val chars = secret.toCharArray()
      try {
        val result = inspect(selection.bytes, chars)
        if (generation.isValid(selection.token)) {
          inspection = result
          if (result is KeystoreInspection.Success) password = ""
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        if (generation.isValid(selection.token)) {
          inspection = KeystoreInspection.Failure(KeystoreError.ParseFailed("Provider could not read this keystore"))
        }
      } finally {
        chars.fill('\u0000')
        if (generation.isValid(selection.token)) isParsing = false
      }
    }
  }

  override fun onCleared() {
    // Entry popped: the store already cancelled the ViewModel scope (aborting
    // in-flight reads/parses). Promptly drop the bytes and wipe every trace
    // of the password, and invalidate the selection so a survivor of the
    // cancelled work can never publish afterwards.
    startSelection()
    isReading = false
    isParsing = false
  }

  private suspend fun read(token: Int, name: String?, readBytes: suspend () -> ByteArray) {
    try {
      val bytes = readBytes()
      if (generation.isValid(token)) {
        loaded = Loaded(token, bytes)
        fileName = name
        canParse = true
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (generation.isValid(token)) {
        error = e.message ?: "Could not read the selected file."
      }
    } finally {
      if (generation.isValid(token)) {
        isReading = false
      }
    }
  }

  /**
   * Invalidates every earlier selection and resets to the "reading" state.
   * Synchronous by contract: the picker callback must not observe stale state.
   */
  private fun startSelection(): Int {
    val token = generation.invalidate()
    loaded = null
    fileName = null
    inspection = null
    password = ""
    error = null
    isParsing = false
    isReading = true
    canParse = false
    return token
  }
}
