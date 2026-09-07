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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.ads.AdSlot
import java.lang.ref.WeakReference

/**
 * play 变体唯一广告形式:激励广告(用户主动看广告换导出次数)。
 * Banner/Native/Interstitial 已在产品决策中移除,保持简单 app 的克制。
 */
object AdmobAdSlot : AdSlot {

  private const val TEST_BANNER_AD_UNIT = "ca-app-pub-3940256099942544/6300978111"

  private var appContext: Context? = null
  private var activityRef: WeakReference<Activity>? = null
  private var rewardedAd: RewardedAd? = null
  private var rewardEarned = false

  private val rewardedReadyFlow = MutableStateFlow(false)
  override val isRewardedReady: StateFlow<Boolean> = rewardedReadyFlow.asStateFlow()

  fun onActivityCreated(activity: Activity) {
    activityRef = WeakReference(activity)
    appContext = activity.applicationContext
  }

  /** 在 UMP 同意流程完成后调用 */
  fun initializeAds() {
    val context = appContext ?: return
    MobileAds.initialize(context) {}
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
        rewardedReadyFlow.value = false
        onResult(rewardEarned)
        preloadRewarded()
      }

      override fun onAdFailedToShowFullScreenContent(error: AdError) {
        rewardedAd = null
        rewardedReadyFlow.value = false
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
          rewardedReadyFlow.value = true
        }
      },
    )
  }
}
