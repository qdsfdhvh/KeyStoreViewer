package signature

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Main-thread selection/read/parse state used by the browser, never saved. */
class KeystoreBrowserState {
  private val generation = SelectionGeneration()
  private data class Loaded(val token: Int, val bytes: ByteArray)
  private var loaded: Loaded? = null

  var fileName by mutableStateOf<String?>(null)
    private set
  var password by mutableStateOf("")
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

  /** Must run synchronously in the accepted picker callback, before launch. */
  fun select(): Int {
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

  fun clear() {
    select()
    isReading = false
  }

  suspend fun read(token: Int, name: String?, readBytes: suspend () -> ByteArray) {
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

  suspend fun parse(
    inspect: (ByteArray, CharArray) -> KeystoreInspection = { bytes, chars ->
      KeystoreInspector.parse(bytes, chars, KeystoreCrypto::providerFor)
    },
  ) {
    val selection = loaded ?: return
    if (!canParse || isReading || isParsing || !generation.isValid(selection.token)) return
    val secret = password
    isParsing = true
    try {
      val result = withContext(Dispatchers.IO) {
        // Allocate inside the dispatched block so cancellation before entry
        // cannot leave a captured char array unwiped. Never log exceptions
        // from providers: their messages may include sensitive input.
        val chars = secret.toCharArray()
        try {
          inspect(selection.bytes, chars)
        } finally {
          chars.fill('\u0000')
        }
      }
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
      if (generation.isValid(selection.token)) isParsing = false
    }
  }
}
