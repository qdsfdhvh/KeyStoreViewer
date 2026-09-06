package com.seiko.keystoreviewer.ads

import android.app.Activity
import platform.ads.AdSlot
import platform.ads.NoAdSlot

/**
 * foss 变体(F-Droid / GitHub Release):无任何广告能力与广告依赖。
 */
object Ads {
  fun initialize(activity: Activity) = Unit

  fun slot(): AdSlot = NoAdSlot
}
