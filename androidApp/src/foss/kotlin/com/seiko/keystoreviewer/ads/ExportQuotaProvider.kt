package com.seiko.keystoreviewer.ads

import android.content.Context
import data.local.ExportQuota
import data.local.UnlimitedExportQuota

object ExportQuotaProvider {
  fun quota(context: Context): ExportQuota = UnlimitedExportQuota
}
