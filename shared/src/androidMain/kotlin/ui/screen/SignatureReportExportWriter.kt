package ui.screen

import android.content.Context
import android.net.Uri
import export.SignatureReportExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android [ExportReportWriter]: builds the CSV from installed user apps and
 * writes it to the picked SAF target on IO. Holds the application context
 * only; never an Activity. Any failure is reported as a failed write (never
 * thrown), matching the previous sheet behavior.
 */
class SignatureReportExportWriter(
  private val applicationContext: Context,
) : ExportReportWriter {

  override suspend fun export(uri: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
      val csv = SignatureReportExporter.buildCsv(applicationContext)
      SignatureReportExporter.write(applicationContext, Uri.parse(uri), csv)
    }.getOrElse { false }
  }
}
