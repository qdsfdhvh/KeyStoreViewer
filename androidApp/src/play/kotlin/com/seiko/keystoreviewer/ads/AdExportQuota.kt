package com.seiko.keystoreviewer.ads

import android.content.Context
import data.local.ExportQuota
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.ads.AdSlot
import java.util.Calendar

/**
 * play 变体的导出配额:每天 [FREE_PER_DAY] 次免费,看激励广告 +[BONUS_PER_AD] 次。
 * 配额按本地时区自然日重置。
 */
class AdExportQuota(context: Context) : ExportQuota {

  private val prefs = context.getSharedPreferences("export_quota", Context.MODE_PRIVATE)

  private val state = MutableStateFlow(currentRemaining())

  override val remaining = state.asStateFlow()

  private val mutex = Mutex()

  override suspend fun tryConsume(): Boolean = mutex.withLock {
    if (currentRemaining() <= 0) return false
    val used = prefs.getInt(KEY_USED, 0)
    val bonus = prefs.getInt(KEY_BONUS, 0)
    prefs.edit()
      .putInt(KEY_DAY, todayKey())
      .apply {
        if (used < FREE_PER_DAY) {
          putInt(KEY_USED, used + 1)
        } else {
          putInt(KEY_BONUS, bonus - 1)
        }
      }
      .apply()
    refresh()
    true
  }

  override suspend fun addBonus(count: Int) {
    mutex.withLock {
      prefs.edit()
        .putInt(KEY_DAY, todayKey())
        .putInt(KEY_BONUS, prefs.getInt(KEY_BONUS, 0) + count)
        .apply()
    }
    refresh()
  }

  private fun currentRemaining(): Int {
    if (prefs.getInt(KEY_DAY, todayKey()) != todayKey()) return FREE_PER_DAY
    val used = prefs.getInt(KEY_USED, 0)
    val bonus = prefs.getInt(KEY_BONUS, 0)
    return (FREE_PER_DAY - used).coerceAtLeast(0) + bonus
  }

  private fun refresh() {
    state.update { currentRemaining() }
  }

  private fun todayKey(): Int = Calendar.getInstance().let {
    it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR)
  }

  companion object {
    const val FREE_PER_DAY = 2
    private const val BONUS_PER_AD = AdSlot.REWARD_BONUS_COUNT

    private const val KEY_DAY = "day"
    private const val KEY_USED = "used"
    private const val KEY_BONUS = "bonus"
  }
}
