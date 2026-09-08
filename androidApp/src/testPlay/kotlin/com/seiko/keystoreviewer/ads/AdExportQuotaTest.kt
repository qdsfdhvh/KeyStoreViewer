package com.seiko.keystoreviewer.ads

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import platform.ads.AdSlot

/**
 * Day-rollover regression for the process-retained [AdExportQuota] instance:
 * an exhausted previous day must un-stick via [AdExportQuota.refresh] even
 * when no ad is ready, and yesterday's used/bonus must reset per the daily
 * policy instead of corrupting the new day's counters.
 */
class AdExportQuotaTest {

  private class FakeStore : QuotaStore {
    val map = mutableMapOf<String, Int>()
    override fun read(key: String, default: Int): Int = map[key] ?: default
    override fun write(vararg keyValues: Pair<String, Int>) {
      keyValues.forEach { (key, value) -> map[key] = value }
    }
  }

  @Test
  fun retainedQuotaUnsticksAfterDayRolloverWithFullDailyReset() = runBlocking {
    var day = 1
    val quota = AdExportQuota(FakeStore()) { day }

    // Day 1: both free exports are spent and the instance is retained with 0.
    assertTrue(quota.tryConsume())
    assertTrue(quota.tryConsume())
    assertFalse(quota.tryConsume())
    assertEquals(0, quota.remaining.first())

    // Day 2 on the SAME instance, ad not ready: only the entry-time refresh
    // re-reads the date-sensitive remaining and re-enables Export.
    day = 2
    quota.refresh()
    assertEquals(AdExportQuota.FREE_PER_DAY, quota.remaining.first())

    // Normalization: yesterday's used=2 must not carry into today's counter
    // (the pre-fix carry bug left remaining at 0 after a single consume).
    assertTrue(quota.tryConsume())
    assertEquals(1, quota.remaining.first())
    assertTrue(quota.tryConsume())
    assertEquals(0, quota.remaining.first())
    assertFalse(quota.tryConsume())
  }

  @Test
  fun unspentBonusResetsWithTheDailyWindowPerPolicy() = runBlocking {
    var day = 1
    val quota = AdExportQuota(FakeStore()) { day }

    // A bonus earned on day 1 but never spent.
    quota.addBonus(AdSlot.REWARD_BONUS_COUNT)
    assertEquals(AdExportQuota.FREE_PER_DAY + AdSlot.REWARD_BONUS_COUNT, quota.remaining.first())

    // It does not carry across the day boundary (daily reset policy).
    day = 2
    quota.refresh()
    assertEquals(AdExportQuota.FREE_PER_DAY, quota.remaining.first())

    // A bonus earned on the new day is fully spendable within that day.
    quota.addBonus(AdSlot.REWARD_BONUS_COUNT)
    assertEquals(AdExportQuota.FREE_PER_DAY + AdSlot.REWARD_BONUS_COUNT, quota.remaining.first())
    repeat(AdExportQuota.FREE_PER_DAY + AdSlot.REWARD_BONUS_COUNT) {
      assertTrue(quota.tryConsume())
    }
    assertEquals(0, quota.remaining.first())
  }

  @Test
  fun sameDayBonusSpendsFromBonusAfterFreeExhaustion() = runBlocking {
    var day = 3
    val quota = AdExportQuota(FakeStore()) { day }

    // Within one day the original semantics are unchanged: free first, then
    // bonus, and consumption fails once everything is spent.
    assertTrue(quota.tryConsume())
    assertTrue(quota.tryConsume())
    assertEquals(0, quota.remaining.first())

    quota.addBonus(AdSlot.REWARD_BONUS_COUNT)
    assertEquals(AdSlot.REWARD_BONUS_COUNT, quota.remaining.first())
    assertTrue(quota.tryConsume())
    assertTrue(quota.tryConsume())
    assertEquals(0, quota.remaining.first())
    assertFalse(quota.tryConsume())

    // A same-day refresh must not resurrect anything.
    quota.refresh()
    assertEquals(0, quota.remaining.first())
  }
}
