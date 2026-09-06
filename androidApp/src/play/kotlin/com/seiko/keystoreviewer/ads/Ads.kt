package com.seiko.keystoreviewer.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import platform.ads.AdSlot

object Ads {
  fun initialize(context: Context) {
    MobileAds.initialize(context) {}
  }

  fun slot(): AdSlot = AdmobAdSlot
}
