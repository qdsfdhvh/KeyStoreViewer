package com.seiko.keystoreviewer.ads

import android.content.Context
import platform.ads.AdSlot
import platform.ads.NoAdSlot

/**
 * foss 变体(F-Droid / GitHub Release):无任何广告能力与广告依赖。
 */
object Ads {
  fun initialize(context: Context) = Unit

  fun slot(): AdSlot = NoAdSlot
}
