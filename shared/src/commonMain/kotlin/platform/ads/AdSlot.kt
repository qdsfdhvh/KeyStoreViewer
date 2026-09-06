package platform.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * 广告展示能力的抽象。
 *
 * 实现按 flavor 隔离在 androidApp 中:play 变体提供 AdMob 实现,
 * foss 变体(F-Droid / GitHub Release)永远只有 [NoAdSlot],
 * 广告 SDK 依赖与代码对 foss 构建物理不可见。
 */
interface AdSlot {
  @Composable fun Banner(modifier: Modifier = Modifier)

  @Composable fun InlineNative(modifier: Modifier = Modifier)

  fun maybeShowInterstitial(placement: String)
}

data object NoAdSlot : AdSlot {
  @Composable override fun Banner(modifier: Modifier) = Unit

  @Composable override fun InlineNative(modifier: Modifier) = Unit

  override fun maybeShowInterstitial(placement: String) = Unit
}

val LocalAdSlot = staticCompositionLocalOf<AdSlot> { NoAdSlot }
