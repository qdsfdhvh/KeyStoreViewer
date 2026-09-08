package com.seiko.keystoreviewer.ads

import android.content.Context
import data.local.ExportQuota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.ads.AdSlot
import java.util.Calendar

/**
 * Minimal storage seam over [SharedPreferences] (one atomic batch per write)
 * so the day-rollover behavior of a retained quota instance is
 * deterministically testable in plain JVM unit tests.
 */
internal interface QuotaStore {
  fun read(key: String, default: Int): Int

  fun write(vararg keyValues: Pair<String, Int>)
}

private class SharedPreferencesQuotaStore(context: Context) : QuotaStore {
  private val prefs = context.getSharedPreferences("export_quota", Context.MODE_PRIVATE)

  override fun read(key: String, default: Int): Int = prefs.getInt(key, default)

  override fun write(vararg keyValues: Pair<String, Int>) {
    prefs.edit().apply {
      keyValues.forEach { (key, value) -> putInt(key, value) }
    }.apply()
  }
}

/**
 * play 变体的导出配额:每天 [FREE_PER_DAY] 次免费,看激励广告 +[BONUS_PER_AD] 次。
 * 配额按本地时区自然日重置:上一天的 used/bonus 不结转;未用完的 bonus 随自然日失效。
 * 实例在 AppSingletons 中进程级保留,[refresh] 在每次弹层进入时重读日期敏感的
 * remaining,避免前一日耗尽后(且广告未就绪时)Export 永久禁用。
 */
class AdExportQuota internal constructor(
  private val store: QuotaStore,
  private val nowDayKey: () -> Int,
) : ExportQuota {

  constructor(context: Context) : this(SharedPreferencesQuotaStore(context), ::defaultTodayKey)

  private val state = MutableStateFlow(currentRemaining())

  override val remaining = state.asStateFlow()

  private val mutex = Mutex()

  override suspend fun tryConsume(): Boolean = mutex.withLock {
    if (currentRemaining() <= 0) return false
    val today = nowDayKey()
    // Rollover normalization: yesterday's used/bonus must not carry into the
    // new day's counters (policy: daily reset).
    val rolledOver = store.read(KEY_DAY, today) != today
    val used = if (rolledOver) 0 else store.read(KEY_USED, 0)
    val bonus = if (rolledOver) 0 else store.read(KEY_BONUS, 0)
    val newUsed: Int
    val newBonus: Int
    if (used < FREE_PER_DAY) {
      newUsed = used + 1
      newBonus = bonus
    } else {
      newUsed = used
      newBonus = bonus - 1
    }
    store.write(KEY_DAY to today, KEY_USED to newUsed, KEY_BONUS to newBonus)
    refreshLocked()
    true
  }

  override suspend fun addBonus(count: Int) {
    mutex.withLock {
      val today = nowDayKey()
      val rolledOver = store.read(KEY_DAY, today) != today
      val bonus = (if (rolledOver) 0 else store.read(KEY_BONUS, 0)) + count
      store.write(
        KEY_DAY to today,
        KEY_USED to if (rolledOver) 0 else store.read(KEY_USED, 0),
        KEY_BONUS to bonus,
      )
      refreshLocked()
    }
  }

  /** Re-reads date-sensitive remaining so a rolled-over day un-sticks Export. */
  override suspend fun refresh() = mutex.withLock {
    refreshLocked()
  }

  private fun currentRemaining(): Int {
    val today = nowDayKey()
    if (store.read(KEY_DAY, today) != today) return FREE_PER_DAY
    val used = store.read(KEY_USED, 0)
    val bonus = store.read(KEY_BONUS, 0)
    return (FREE_PER_DAY - used).coerceAtLeast(0) + bonus
  }

  private fun refreshLocked() {
    state.value = currentRemaining()
  }

  internal companion object {
    const val FREE_PER_DAY = 2
    private const val BONUS_PER_AD = AdSlot.REWARD_BONUS_COUNT

    private const val KEY_DAY = "day"
    private const val KEY_USED = "used"
    private const val KEY_BONUS = "bonus"

    private fun defaultTodayKey(): Int = Calendar.getInstance().let {
      it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR)
    }
  }
}
