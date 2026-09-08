package ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import signature.ApkSignerRead
import signature.ApkSignerReadError
import signature.SelectionGeneration

/** Which of the two comparison slots a picked APK belongs to. */
enum class ApkCompareSide {
  Left,
  Right,
}

/**
 * Reads the signer metadata of the freshly picked APK for one comparison
 * side; must be main-safe. Implementations own their temporary files: they
 * delete them on success, failure, replacement and cancellation.
 */
fun interface ApkCompareSource {
  suspend fun read(): ApkSignerRead
}

/**
 * Lifecycle-owned state of the dual-APK signer comparison.
 *
 * Each side keeps its own [SelectionGeneration]: a newer pick supersedes the
 * side's in-flight read (the stale read is cancelled - its temporary copy is
 * deleted by the source's cleanup - and can never publish), so a result is
 * only ever rendered under the file it was read from.
 */
class ApkCompareViewModel : ViewModel() {

  private val generations = mapOf(
    ApkCompareSide.Left to SelectionGeneration(),
    ApkCompareSide.Right to SelectionGeneration(),
  )
  private val jobs = mutableMapOf<ApkCompareSide, Job>()

  private val _left = MutableStateFlow<ApkSignerRead?>(null)

  /** null while nothing was picked (yet) for the left side. */
  val left: StateFlow<ApkSignerRead?> = _left.asStateFlow()

  private val _right = MutableStateFlow<ApkSignerRead?>(null)

  /** null while nothing was picked (yet) for the right side. */
  val right: StateFlow<ApkSignerRead?> = _right.asStateFlow()

  fun onApkPicked(side: ApkCompareSide, source: ApkCompareSource) {
    val generation = generations.getValue(side)
    val token = generation.invalidate()
    // A superseded read is cancelled; its temporary APK copy is deleted by
    // the source's cleanup, never kept.
    jobs.remove(side)?.cancel()
    setResult(side, null)
    jobs[side] = viewModelScope.launch {
      val read = try {
        source.read()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        ApkSignerRead.Failure(ApkSignerReadError.NotApkOrUnreadable)
      }
      // A newer selection for this side supersedes this result.
      if (generation.isValid(token)) {
        setResult(side, read)
      }
    }
  }

  private fun setResult(side: ApkCompareSide, read: ApkSignerRead?) {
    when (side) {
      ApkCompareSide.Left -> _left.value = read
      ApkCompareSide.Right -> _right.value = read
    }
  }
}
