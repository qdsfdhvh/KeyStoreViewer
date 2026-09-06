package com.seiko.keystoreviewer.ads

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import platform.ads.AdSlot

object Ads {
  fun initialize(activity: Activity) {
    AdmobAdSlot.onActivityCreated(activity)
    MobileAds.initialize(activity) {}
  }

  fun slot(): AdSlot = AdmobAdSlot
}
