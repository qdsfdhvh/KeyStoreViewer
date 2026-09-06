package com.seiko.keystoreviewer.ads

import android.content.Context
import data.local.ExportQuota

object ExportQuotaProvider {
  fun quota(context: Context): ExportQuota = AdExportQuota(context)
}
