package com.seiko.keystoreviewer.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class UpdateStatus { Idle, Pending, Downloading, Downloaded, Installing, Installed, Failed, Canceled }

/** The optional consent action wraps a single-use AppUpdateInfo; never cache it across attempts. */
internal data class UpdateOffer(val status: UpdateStatus, val launchConsent: (() -> Boolean)? = null)

internal interface UpdateClient {
  fun observe(listener: (UpdateStatus) -> Unit)
  fun stopObserving()
  fun check(result: (Result<UpdateOffer>) -> Unit)
  fun complete(result: (Result<Unit>) -> Unit)
}

internal sealed interface UpdateUi {
  data object Hidden : UpdateUi
  data object Available : UpdateUi
  data object Checking : UpdateUi
  data object Ready : UpdateUi
  data object Installing : UpdateUi
  data class Error(val completing: Boolean) : UpdateUi
}

/** Main-thread controller; stale foreground/query callbacks cannot override newer install events. */
internal class FlexibleUpdateController(private val client: UpdateClient) {
  private val mutableState = MutableStateFlow<UpdateUi>(UpdateUi.Hidden)
  val state = mutableState.asStateFlow()
  private var active = false
  private var generation = 0
  private var revision = 0
  private var availableDismissed = false
  private var readyDismissed = false

  fun resume() {
    if (active) return
    active = true
    generation++
    readyDismissed = false
    val session = generation
    client.observe { status ->
      if (active && generation == session) {
        revision++
        showStatus(status)
      }
    }
    check(manual = false)
  }

  fun pause() {
    if (!active) return
    active = false
    generation++
    client.stopObserving()
    if (mutableState.value == UpdateUi.Checking) mutableState.value = UpdateUi.Hidden
  }

  fun dismiss() {
    revision++ // A queued Later also cancels a manual check that has not launched consent yet.
    availableDismissed = true
    readyDismissed = true
    mutableState.value = UpdateUi.Hidden
  }

  fun requestUpdate() {
    if (!active) return
    if (mutableState.value != UpdateUi.Available && mutableState.value != UpdateUi.Error(completing = false)) return
    check(manual = true)
  }

  fun onConsentResult(accepted: Boolean, failed: Boolean = false) {
    // Returning from Play must not immediately prompt again, or erase an already downloaded update.
    availableDismissed = true
    if (!accepted && mutableState.value != UpdateUi.Ready) {
      mutableState.value = if (failed) UpdateUi.Error(completing = false) else UpdateUi.Hidden
    }
  }

  fun restart() {
    if (!active || mutableState.value != UpdateUi.Ready) return
    mutableState.value = UpdateUi.Installing
    val session = generation
    client.complete { result ->
      if (active && generation == session && result.isFailure) {
        mutableState.value = UpdateUi.Error(completing = true)
      }
    }
  }

  fun retry() {
    val error = mutableState.value as? UpdateUi.Error ?: return
    if (error.completing) {
      mutableState.value = UpdateUi.Ready
      restart()
    } else {
      requestUpdate()
    }
  }

  private fun check(manual: Boolean) {
    if (manual) mutableState.value = UpdateUi.Checking
    val session = generation
    val request = ++revision
    client.check callback@{ result ->
      if (!active || generation != session || revision != request) return@callback
      val offer = result.getOrNull()
      if (offer == null) {
        // No Play Store, offline, or sideloaded builds must never block ordinary app use.
        if (manual) mutableState.value = UpdateUi.Error(completing = false)
        return@callback
      }
      if (offer.status in setOf(UpdateStatus.Downloaded, UpdateStatus.Pending, UpdateStatus.Downloading, UpdateStatus.Installing, UpdateStatus.Installed)) {
        showStatus(offer.status)
      } else if (manual && offer.launchConsent != null) {
        availableDismissed = true
        mutableState.value = UpdateUi.Hidden
        val launched = runCatching { offer.launchConsent.invoke() }.getOrDefault(false)
        if (!launched) mutableState.value = UpdateUi.Error(completing = false)
      } else {
        mutableState.value = if (!availableDismissed && offer.launchConsent != null) UpdateUi.Available else UpdateUi.Hidden
      }
    }
  }

  private fun showStatus(status: UpdateStatus) {
    mutableState.value = when (status) {
      UpdateStatus.Downloaded -> if (readyDismissed) UpdateUi.Hidden else UpdateUi.Ready
      UpdateStatus.Failed -> UpdateUi.Error(completing = false)
      else -> UpdateUi.Hidden
    }
  }
}
