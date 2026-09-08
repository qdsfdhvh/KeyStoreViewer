package ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.local.ExportQuota
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.ads.AdSlot

/**
 * Writes the signature CSV report to the SAF target picked by the user; must
 * be main-safe. Android implementation backs this with the
 * [export.SignatureReportExporter] and the application context.
 */
fun interface ExportReportWriter {
  /** @return true only when the whole report was written. */
  suspend fun export(uri: String): Boolean
}

/** One-shot requests from the ViewModel to the sheet UI. */
sealed interface ExportEvent {
  /** A slot was spent: the sheet may open the system file creator. */
  data object CreateDocument : ExportEvent

  /** No free slot: the sheet should offer a rewarded ad. */
  data object OutOfQuota : ExportEvent

  /** The write attempt finished; [ok] mirrors whether saving succeeded. */
  data class WriteFinished(val ok: Boolean) : ExportEvent

  /** The earned bonus was credited but no slot could be spent. */
  data object BonusWithoutSlot : ExportEvent
}

/** Internal: routes an [ExportEvent] to the sheet session that produced it. */
private data class SessionEvent(val originSession: Long, val event: ExportEvent)

/**
 * Lifecycle-owned business work of the export sheet: quota consumption,
 * rewarded-ad crediting and the report write.
 *
 * Sheet sessions: the retained entry-scoped VM can outlive a sheet instance
 * (Activity recreation resets the sheet's transient state). Every sheet
 * instance opens a [session][beginSession]; events are delivered only to the
 * still-current session, so in-flight producers from a detached session can
 * never drive a fresh sheet's UI (stale toasts, instant dismiss or an
 * unexpected document-creator launch).
 *
 * Durable work: a started report write finishes even if the entry-scoped store
 * clears this VM mid-write ([NonCancellable]); bonus crediting likewise
 * completes once started. But the ad callback itself must arrive while this VM
 * is still alive: if the entry (and its viewModelScope) is cleared first, the
 * pending reward cannot be credited at all. Quota semantics (tryConsume /
 * addBonus / remaining / refresh) are unchanged across FOSS (unlimited) and
 * Play (daily + ad bonus) builds. Pure sheet UI state (dialogs, busy flags)
 * stays local.
 */
class ExportViewModel(
  private val quota: ExportQuota,
  private val writer: ExportReportWriter,
) : ViewModel() {

  private val _events = Channel<SessionEvent>(Channel.UNLIMITED)
  val events: Flow<ExportEvent> = _events.receiveAsFlow()
    // Routing decision at delivery time: only the session that is still
    // current receives its events.
    .filter { it.originSession == currentSessionId }
    .map { it.event }

  /**
   * Identity of the currently-collecting sheet session. Accessed only from
   * the main dispatcher (UI invocations, viewModelScope on Main, collectors),
   * so a plain counter is coherent.
   */
  private var currentSessionId = 0L

  /** The session an interactive sheet belongs to; captured for deferred callbacks. */
  val session: Long get() = currentSessionId

  /**
   * Opens a fresh sheet session: supersedes any older session (their queued
   * and in-flight events are no longer deliverable) and drops the backlog so
   * it cannot replay into the new session.
   */
  fun beginSession(): Long {
    currentSessionId += 1
    while (_events.tryReceive().isSuccess) {
      // Drain the stale backlog until the channel is empty.
    }
    return currentSessionId
  }

  /** Ends only the originating sheet's session; newer sessions stay active. */
  fun endSession(originSession: Long) {
    if (originSession == currentSessionId) {
      currentSessionId += 1
    }
  }

  /** Re-reads date-sensitive remaining (day rollover) on sheet entry. */
  fun refreshQuota() {
    viewModelScope.launch {
      quota.refresh()
    }
  }

  /** Spends one slot if available; otherwise asks the UI to offer a rewarded ad. */
  fun exportClicked() {
    // Invoked by the currently interactive sheet, which is by definition the
    // current session.
    val originSession = currentSessionId
    viewModelScope.launch {
      if (quota.tryConsume()) {
        _events.send(SessionEvent(originSession, ExportEvent.CreateDocument))
      } else {
        _events.send(SessionEvent(originSession, ExportEvent.OutOfQuota))
      }
    }
  }

  /** Writes the report even if the entry-scoped store clears this VM mid-write. */
  fun writeReport(uri: String) {
    // Invoked by the current sheet's launcher callback; a result redelivered
    // to a newer session's launcher belongs to that newer session.
    val originSession = currentSessionId
    viewModelScope.launch {
      val ok = withContext(NonCancellable) {
        writer.export(uri)
      }
      _events.send(SessionEvent(originSession, ExportEvent.WriteFinished(ok)))
    }
  }

  /**
   * Credits the earned bonus for a fully watched ad, then spends one slot if
   * possible. [originSession] is the session whose user watched the ad,
   * captured by the UI when the ad started: crediting is durable (once this
   * runs, it completes even if the entry-scoped store clears this VM
   * mid-credit), but only that still-current session spends a slot and
   * receives the follow-up event - a detached session neither hijacks the
   * reopened sheet nor burns its quota.
   */
  fun rewardEarned(originSession: Long) {
    viewModelScope.launch {
      withContext(NonCancellable) {
        quota.addBonus(AdSlot.REWARD_BONUS_COUNT)
      }
      if (originSession == currentSessionId && quota.tryConsume()) {
        _events.send(SessionEvent(originSession, ExportEvent.CreateDocument))
      } else if (originSession == currentSessionId) {
        _events.send(SessionEvent(originSession, ExportEvent.BonusWithoutSlot))
      }
    }
  }
}
