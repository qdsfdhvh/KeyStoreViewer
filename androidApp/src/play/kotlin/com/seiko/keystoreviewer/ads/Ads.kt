package com.seiko.keystoreviewer.ads

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import platform.ads.AdSlot

object Ads {

  private var adsInitialized = false

  fun initialize(activity: Activity) {
    AdmobAdSlot.onActivityCreated(activity)

    val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
    consentInformation.requestConsentInfoUpdate(
      activity,
      ConsentRequestParameters.Builder().build(),
      {
        if (consentInformation.canRequestAds()) initAds(activity)
      },
      {
        // 同意信息获取失败(如离线):不初始化广告,其余功能不受影响
      },
    )
    // 上一会话已收集过同意时,无需等本次请求即可初始化
    if (consentInformation.canRequestAds()) initAds(activity)
  }

  private fun initAds(activity: Activity) {
    if (adsInitialized) return
    adsInitialized = true
    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {}
    AdmobAdSlot.initializeAds()
  }

  fun slot(): AdSlot = AdmobAdSlot
}
