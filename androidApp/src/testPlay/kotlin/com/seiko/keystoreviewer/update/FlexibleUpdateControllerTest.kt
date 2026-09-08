package com.seiko.keystoreviewer.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlexibleUpdateControllerTest {
  private val client = FakeClient()
  private val controller = FlexibleUpdateController(client)
  private var launches = 0
  private fun offer(status: UpdateStatus = UpdateStatus.Idle, succeeds: Boolean = true) = UpdateOffer(status) {
    launches++
    succeeds
  }

  @Test fun availabilityNeverLaunchesConsentAutomatically() {
    controller.resume()
    client.answer(0, offer())
    assertEquals(UpdateUi.Available, controller.state.value)
    assertEquals(0, launches)
  }

  @Test fun tapRequestsFreshInfoAndDeduplicatesDoubleTap() {
    controller.resume()
    client.answer(0, offer())
    controller.requestUpdate()
    controller.requestUpdate()
    assertEquals(2, client.checks.size)
    assertEquals(0, launches)
    client.answer(1, offer())
    assertEquals(1, launches)
    assertEquals(UpdateUi.Hidden, controller.state.value)
  }

  @Test fun unavailableOrDisallowedUpdateStaysHidden() {
    controller.resume()
    client.answer(0, UpdateOffer(UpdateStatus.Idle))
    assertEquals(UpdateUi.Hidden, controller.state.value)
    assertEquals(0, launches)
  }

  @Test fun noPlayOrNetworkFailureDoesNotBlockTheApp() {
    controller.resume()
    client.checks[0](Result.failure(IllegalStateException("Unavailable")))
    assertEquals(UpdateUi.Hidden, controller.state.value)
  }

  @Test fun laterAndCancelDoNotRepromptOnResume() {
    controller.resume()
    client.answer(0, offer())
    controller.dismiss()
    controller.pause()
    controller.resume()
    client.answer(1, offer())
    assertEquals(UpdateUi.Hidden, controller.state.value)
    controller.onConsentResult(false)
    assertEquals(0, launches)
  }

  @Test fun playConsentFailureOffersRetryInsteadOfTreatingItAsCancellation() {
    controller.resume()
    client.answer(0, offer())
    controller.requestUpdate()
    client.answer(1, offer())
    controller.onConsentResult(accepted = false, failed = true)
    assertEquals(UpdateUi.Error(false), controller.state.value)
    controller.retry()
    assertEquals(3, client.checks.size)
  }

  @Test fun downloadedOnResumeRequiresExplicitRestart() {
    controller.resume()
    client.answer(0, offer(UpdateStatus.Downloaded))
    assertEquals(UpdateUi.Ready, controller.state.value)
    assertEquals(0, client.completions.size)
    assertEquals(0, launches)
    controller.restart()
    controller.restart()
    assertEquals(1, client.completions.size)
    assertEquals(UpdateUi.Installing, controller.state.value)
  }

  @Test fun downloadedLaterIsOfferedOnNextForeground() {
    controller.resume()
    client.answer(0, UpdateOffer(UpdateStatus.Downloaded))
    controller.dismiss()
    client.listener!!(UpdateStatus.Downloaded)
    assertEquals(UpdateUi.Hidden, controller.state.value)
    controller.pause()
    controller.resume()
    client.answer(1, UpdateOffer(UpdateStatus.Downloaded))
    assertEquals(UpdateUi.Ready, controller.state.value)
  }

  @Test fun backgroundDownloadDoesNotLaunchOrRestartAnything() {
    controller.resume()
    client.answer(0, offer(UpdateStatus.Downloading))
    client.listener!!(UpdateStatus.Downloaded)
    assertEquals(UpdateUi.Ready, controller.state.value)
    controller.onConsentResult(true)
    assertEquals(UpdateUi.Ready, controller.state.value)
    assertEquals(0, launches)
    assertTrue(client.completions.isEmpty())
  }

  @Test fun newerDownloadEventWinsOverOldCheck() {
    controller.resume()
    client.listener!!(UpdateStatus.Downloaded)
    client.answer(0, offer())
    assertEquals(UpdateUi.Ready, controller.state.value)
  }

  @Test fun listenerAndCallbacksAreBoundToForegroundLifetime() {
    controller.resume()
    controller.resume()
    assertEquals(1, client.registrations)
    val oldListener = client.listener!!
    controller.pause()
    controller.pause()
    assertEquals(1, client.unregistrations)
    client.answer(0, offer())
    oldListener(UpdateStatus.Downloaded)
    assertEquals(UpdateUi.Hidden, controller.state.value)
    controller.resume()
    oldListener(UpdateStatus.Downloaded)
    client.answer(1, UpdateOffer(UpdateStatus.Idle))
    assertEquals(UpdateUi.Hidden, controller.state.value)
  }

  @Test fun failedLaunchRetryUsesAnotherOffer() {
    controller.resume()
    client.answer(0, offer())
    controller.requestUpdate()
    client.answer(1, offer(succeeds = false))
    assertEquals(UpdateUi.Error(false), controller.state.value)
    controller.retry()
    assertEquals(3, client.checks.size)
    client.answer(2, offer())
    assertEquals(2, launches)
  }

  @Test fun downloadFailureCanRetryWithNewEligibleOffer() {
    controller.resume()
    client.answer(0, offer(UpdateStatus.Downloading))
    client.listener!!(UpdateStatus.Failed)
    controller.retry()
    client.answer(1, offer(UpdateStatus.Failed))
    assertEquals(1, launches)
  }

  @Test fun completionFailureCanRetryButNeverInstallsWhilePaused() {
    controller.resume()
    client.answer(0, UpdateOffer(UpdateStatus.Downloaded))
    controller.restart()
    client.completions[0](Result.failure(IllegalStateException("Install failed")))
    assertEquals(UpdateUi.Error(true), controller.state.value)
    controller.retry()
    assertEquals(2, client.completions.size)
    controller.pause()
    controller.restart()
    assertEquals(2, client.completions.size)
  }

  @Test fun manualCheckFailureIsRetryableAndStaleRequestCannotOverwriteIt() {
    controller.resume()
    client.answer(0, offer())
    controller.pause()
    controller.resume()
    controller.requestUpdate()
    client.checks[2](Result.failure(IllegalStateException("Offline")))
    client.answer(1, offer())
    assertEquals(UpdateUi.Error(false), controller.state.value)
    controller.retry()
    assertEquals(4, client.checks.size)
  }

  @Test fun queuedTapAfterConsentLaunchDoesNotStartAnotherAttempt() {
    controller.resume()
    client.answer(0, offer())
    controller.requestUpdate()
    client.answer(1, offer())
    controller.requestUpdate()
    assertEquals(2, client.checks.size)
    assertEquals(1, launches)
  }

  @Test fun laterCancelsInFlightManualCheck() {
    controller.resume()
    client.answer(0, offer())
    controller.requestUpdate()
    controller.dismiss()
    client.answer(1, offer())
    assertEquals(UpdateUi.Hidden, controller.state.value)
    assertEquals(0, launches)
  }

  @Test fun downloadedBetweenBannerAndTapDoesNotLaunchConsentAgain() {
    controller.resume()
    client.answer(0, offer())
    controller.requestUpdate()
    client.answer(1, offer(UpdateStatus.Downloaded))
    assertEquals(UpdateUi.Ready, controller.state.value)
    assertEquals(0, launches)
  }

  private class FakeClient : UpdateClient {
    var listener: ((UpdateStatus) -> Unit)? = null
    var registrations = 0
    var unregistrations = 0
    val checks = mutableListOf<(Result<UpdateOffer>) -> Unit>()
    val completions = mutableListOf<(Result<Unit>) -> Unit>()
    override fun observe(listener: (UpdateStatus) -> Unit) {
      registrations++
      this.listener = listener
    }
    override fun stopObserving() {
      unregistrations++
      listener = null
    }
    override fun check(result: (Result<UpdateOffer>) -> Unit) {
      checks += result
    }
    override fun complete(result: (Result<Unit>) -> Unit) {
      completions += result
    }
    fun answer(index: Int, offer: UpdateOffer) {
      checks[index](Result.success(offer))
    }
  }
}
