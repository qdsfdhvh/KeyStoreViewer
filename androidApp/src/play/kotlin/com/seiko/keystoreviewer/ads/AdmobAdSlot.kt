package com.seiko.keystoreviewer.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import platform.ads.AdSlot

/**
 * M1 阶段使用 Google 官方测试 ID;正式 ad unit ID 在 M3 接入,
 * 届时经 play 变体 BuildConfig 注入,不落仓库。
 */
object AdmobAdSlot : AdSlot {

  private const val TEST_BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"

  @Composable
  override fun Banner(modifier: Modifier) {
    AndroidView(
      modifier = modifier.fillMaxWidth(),
      factory = { context ->
        AdView(context).apply {
          setAdSize(AdSize.BANNER)
          adUnitId = TEST_BANNER_AD_UNIT
        }
      },
      update = { adView -> adView.loadAd(AdRequest.Builder().build()) },
    )
  }

  @Composable
  override fun InlineNative(modifier: Modifier) = Unit

  override fun maybeShowInterstitial(placement: String) = Unit
}
