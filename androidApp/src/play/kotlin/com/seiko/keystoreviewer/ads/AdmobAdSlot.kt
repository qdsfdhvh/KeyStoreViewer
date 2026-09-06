package com.seiko.keystoreviewer.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.seiko.keystoreviewer.BuildConfig
import platform.ads.AdSlot
import java.lang.ref.WeakReference

/**
 * M1 阶段 Banner 使用 Google 官方测试 ID;正式 banner/native/interstitial ID 在 M3 接入。
 * Rewarded ID 经 play 变体 BuildConfig 注入(不入 git)。
 */
object AdmobAdSlot : AdSlot {

  private const val TEST_BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"

  private var appContext: Context? = null
  private var activityRef: WeakReference<Activity>? = null
  private var rewardedAd: RewardedAd? = null
  private var rewardEarned = false

  fun onActivityCreated(activity: Activity) {
    activityRef = WeakReference(activity)
    appContext = activity.applicationContext
    MobileAds.initialize(activity) {}
    preloadRewarded()
  }

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

  override fun showRewarded(placement: String, onResult: (rewarded: Boolean) -> Unit) {
    val activity = activityRef?.get()
    val ad = rewardedAd
    if (activity == null || ad == null) {
      onResult(false)
      preloadRewarded()
      return
    }
    rewardEarned = false
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        rewardedAd = null
        onResult(rewardEarned)
        preloadRewarded()
      }

      override fun onAdFailedToShowFullScreenContent(error: AdError) {
        rewardedAd = null
        onResult(false)
        preloadRewarded()
      }
    }
    ad.show(activity) { rewardEarned = true }
  }

  override fun canShowRewarded(): Boolean = true

  private fun preloadRewarded() {
    val context = appContext ?: return
    RewardedAd.load(
      context,
      BuildConfig.ADMOB_REWARDED_ID,
      AdRequest.Builder().build(),
      object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) {
          rewardedAd = ad
        }
      },
    )
  }
}
