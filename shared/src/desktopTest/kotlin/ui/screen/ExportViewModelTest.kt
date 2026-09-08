package ui.screen

import androidx.lifecycle.viewModelScope
import data.local.ExportQuota
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import platform.ads.AdSlot

class ExportViewModelTest {

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private class FakeExportQuota : ExportQuota {
    val state = MutableStateFlow(2)

    override val remaining: Flow<Int> = state

    var consumeGate: CompletableDeferred<Unit>? = null
    var bonusGate: CompletableDeferred<Unit>? = null
    var canConsume = true

    val consumedCount get() = consumed
    val bonusTotal get() = bonus
    val refreshCount get() = refreshes

    private var consumed = 0
    private var bonus = 0
    private var refreshes = 0

    override suspend fun tryConsume(): Boolean {
      consumeGate?.await()
      if (!canConsume) return false
      consumed++
      state.value = state.value - 1
      return true
    }

    override suspend fun addBonus(count: Int) {
      bonusGate?.await()
      bonus += count
      state.value = state.value + count
    }

    override suspend fun refresh() {
      refreshes++
    }
  }

  private class FakeWriter : ExportReportWriter {
    var gate: CompletableDeferred<Unit>? = null
    var ok = true
    val writtenUris = mutableListOf<String>()

    override suspend fun export(uri: String): Boolean {
      gate?.await()
      writtenUris += uri
      return ok
    }
  }

  private fun TestScope.collectEvents(
    viewModel: ExportViewModel,
    events: MutableList<ExportEvent>,
  ) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.events.collect { events += it }
    }
  }

  @Test
  fun exportClickConsumesOneSlotAndOpensTheDocumentCreator() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota()
    val viewModel = ExportViewModel(quota, FakeWriter())
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    viewModel.exportClicked()
    advanceUntilIdle()

    assertEquals(1, quota.consumedCount)
    assertEquals(listOf<ExportEvent>(ExportEvent.CreateDocument), events)
  }

  @Test
  fun exportClickWithoutQuotaAsksForARewardedAdWithoutConsuming() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota().apply { canConsume = false }
    val viewModel = ExportViewModel(quota, FakeWriter())
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    viewModel.exportClicked()
    advanceUntilIdle()

    assertEquals(listOf<ExportEvent>(ExportEvent.OutOfQuota), events)
    assertEquals(0, quota.consumedCount)
  }

  @Test
  fun rewardEarnedCreditsBonusThenSpendsOneSlot() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota().apply { canConsume = false }
    val viewModel = ExportViewModel(quota, FakeWriter())
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    // First attempt fails (this is what offered the ad in the UI).
    viewModel.exportClicked()
    advanceUntilIdle()
    assertEquals(listOf<ExportEvent>(ExportEvent.OutOfQuota), events)

    // The user finished the ad: the bonus is credited and one slot spent.
    quota.canConsume = true
    viewModel.rewardEarned(viewModel.session)
    advanceUntilIdle()

    assertEquals(AdSlot.REWARD_BONUS_COUNT, quota.bonusTotal)
    assertEquals(1, quota.consumedCount)
    assertEquals(
      listOf<ExportEvent>(ExportEvent.OutOfQuota, ExportEvent.CreateDocument),
      events,
    )
  }

  @Test
  fun rewardEarnedKeepsTheBonusWhenNoSlotCanBeSpent() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota().apply { canConsume = false }
    val viewModel = ExportViewModel(quota, FakeWriter())
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    viewModel.rewardEarned(viewModel.session)
    advanceUntilIdle()

    assertEquals(AdSlot.REWARD_BONUS_COUNT, quota.bonusTotal) // bonus is never lost
    assertEquals(listOf<ExportEvent>(ExportEvent.BonusWithoutSlot), events)
  }

  @Test
  fun writeReportWritesThePickedUriAndReportsTheResult() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val writer = FakeWriter()
    val viewModel = ExportViewModel(FakeExportQuota(), writer)
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    viewModel.writeReport("content://docs/1")
    advanceUntilIdle()
    assertEquals(listOf<ExportEvent>(ExportEvent.WriteFinished(true)), events)
    assertEquals(listOf("content://docs/1"), writer.writtenUris)

    writer.ok = false
    viewModel.writeReport("content://docs/2")
    advanceUntilIdle()
    assertEquals(
      listOf<ExportEvent>(ExportEvent.WriteFinished(true), ExportEvent.WriteFinished(false)),
      events,
    )
    assertEquals(2, writer.writtenUris.size)
  }

  @Test
  fun startedWriteCompletesEvenIfTheEntryScopedScopeIsCancelledMidWrite() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val writer = FakeWriter().apply { gate = CompletableDeferred() }
    val viewModel = ExportViewModel(FakeExportQuota(), writer)
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    viewModel.writeReport("content://docs/1")
    viewModel.viewModelScope.cancel()
    writer.gate?.complete(Unit)
    advanceUntilIdle()

    // The quota was already spent: the write must complete regardless.
    assertEquals(listOf("content://docs/1"), writer.writtenUris)
  }

  @Test
  fun freshSheetSessionDiscardsEventsQueuedWhileNoSheetWasCollecting() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota().apply { canConsume = false }
    val viewModel = ExportViewModel(quota, FakeWriter())
    // No collector: the sheet is closed while the entry retains this VM
    // (e.g. dismissed mid-write after a configuration change).
    viewModel.exportClicked()
    viewModel.rewardEarned(viewModel.session)
    advanceUntilIdle()
    assertEquals(AdSlot.REWARD_BONUS_COUNT, quota.bonusTotal) // the durable side effects still happened

    // A new sheet session supersedes the detached one: the stale backlog is
    // dropped and only this session's events are delivered.
    viewModel.beginSession()
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)
    assertEquals(emptyList<ExportEvent>(), events)

    viewModel.exportClicked()
    advanceUntilIdle()
    assertEquals(listOf<ExportEvent>(ExportEvent.OutOfQuota), events)
  }

  /**
   * Gated repro of the P1 finding: session A's write is still in flight when
   * session B opens; A finishes only afterwards. The durable write completes,
   * but its completion must not reach (and dismiss) the reopened session.
   * Demonstrated RED on the pre-session-routing implementation (the stale
   * WriteFinished was delivered to B).
   */
  @Test
  fun oldWriteFinishingAfterReopenMustNotCloseTheReopenedSheet() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val writer = FakeWriter().apply { gate = CompletableDeferred() }
    val viewModel = ExportViewModel(FakeExportQuota(), writer)
    // Session A starts a write, then detaches (Activity recreation closed the sheet).
    viewModel.writeReport("content://docs/A")
    // Session B reopens and takes ownership.
    viewModel.beginSession()
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)
    // A's in-flight write finishes only AFTER B is collecting.
    writer.gate?.complete(Unit)
    advanceUntilIdle()
    // The durable write still completes...
    assertEquals(listOf("content://docs/A"), writer.writtenUris)
    // ...but its completion must not drive session B's UI.
    assertEquals(emptyList<ExportEvent>(), events)
  }

  /** A current-session write finishing while its own sheet collects is still delivered. */
  @Test
  fun writeFinishingWithinItsOwnSessionIsStillDelivered() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val writer = FakeWriter().apply { gate = CompletableDeferred() }
    val viewModel = ExportViewModel(FakeExportQuota(), writer)
    viewModel.beginSession()
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    viewModel.writeReport("content://docs/B")
    writer.gate?.complete(Unit)
    advanceUntilIdle()

    assertEquals(listOf("content://docs/B"), writer.writtenUris)
    assertEquals(listOf<ExportEvent>(ExportEvent.WriteFinished(true)), events)
  }

  /**
   * Delayed reward across sessions: session A's fully watched ad credits its
   * bonus even though the callback arrives after session B took ownership,
   * but it neither burns B's next slot nor emits anything into B.
   */
  @Test
  fun delayedRewardFromASupersededSessionCreditsBonusWithoutDrivingTheNewSession() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota()
    val viewModel = ExportViewModel(quota, FakeWriter())
    val sessionA = viewModel.session
    // Session B reopens; session A's ad callback fires afterwards.
    viewModel.beginSession()
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    viewModel.rewardEarned(sessionA)
    advanceUntilIdle()

    assertEquals(AdSlot.REWARD_BONUS_COUNT, quota.bonusTotal) // the watched ad's bonus persists
    assertEquals(0, quota.consumedCount) // no slot burned for a detached session
    assertEquals(emptyList<ExportEvent>(), events) // nothing drives the new session

    // The preserved bonus is spendable in the current session's next attempt.
    viewModel.exportClicked()
    advanceUntilIdle()
    assertEquals(listOf<ExportEvent>(ExportEvent.CreateDocument), events)
    assertEquals(1, quota.consumedCount)
  }

  @Test
  fun rewardAfterDisposalBeforeReopenCreditsWithoutSpending() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota()
    val viewModel = ExportViewModel(quota, FakeWriter())
    val sessionA = viewModel.beginSession()

    viewModel.endSession(sessionA)
    viewModel.rewardEarned(sessionA)
    advanceUntilIdle()

    assertEquals(AdSlot.REWARD_BONUS_COUNT, quota.bonusTotal)
    assertEquals(0, quota.consumedCount)
    viewModel.beginSession()
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)
    assertEquals(emptyList<ExportEvent>(), events)
    viewModel.exportClicked()
    advanceUntilIdle()
    assertEquals(1, quota.consumedCount)
    assertEquals(listOf<ExportEvent>(ExportEvent.CreateDocument), events)
  }

  @Test
  fun oldSessionDisposalDoesNotInvalidateNewSession() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ExportViewModel(FakeExportQuota(), FakeWriter())
    val sessionA = viewModel.beginSession()
    val sessionB = viewModel.beginSession()
    viewModel.endSession(sessionA)
    assertEquals(sessionB, viewModel.session)
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)
    viewModel.exportClicked()
    advanceUntilIdle()
    assertEquals(listOf<ExportEvent>(ExportEvent.CreateDocument), events)
  }

  @Test
  fun disposalDropsCompletionWithoutCancellingStartedWrite() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val writer = FakeWriter().apply { gate = CompletableDeferred() }
    val viewModel = ExportViewModel(FakeExportQuota(), writer)
    val sessionA = viewModel.beginSession()
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)
    viewModel.writeReport("content://docs/A")
    viewModel.endSession(sessionA)
    writer.gate?.complete(Unit)
    advanceUntilIdle()
    assertEquals(listOf("content://docs/A"), writer.writtenUris)
    assertEquals(emptyList<ExportEvent>(), events)
  }

  /** Sheet entry refreshes the date-sensitive quota on the SAME retained instance. */
  @Test
  fun sheetEntryRefreshesDateSensitiveQuota() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val quota = FakeExportQuota()
    val viewModel = ExportViewModel(quota, FakeWriter())

    viewModel.beginSession()
    viewModel.refreshQuota()
    advanceUntilIdle()

    assertEquals(1, quota.refreshCount)
  }

  @Test
  fun unlimitedQuotaFlowAllowsEveryExport() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = ExportViewModel(UnlimitedTestQuota(), FakeWriter())
    val events = mutableListOf<ExportEvent>()
    collectEvents(viewModel, events)

    repeat(3) { viewModel.exportClicked() }
    advanceUntilIdle()

    assertEquals(List(3) { ExportEvent.CreateDocument }, events)
  }
}

/** Mirrors [data.local.UnlimitedExportQuota] semantics for the FOSS build. */
private class UnlimitedTestQuota : ExportQuota {
  override val remaining: Flow<Int> = MutableStateFlow(Int.MAX_VALUE)

  override suspend fun tryConsume(): Boolean = true

  override suspend fun addBonus(count: Int) = Unit

  override suspend fun refresh() = Unit
}
