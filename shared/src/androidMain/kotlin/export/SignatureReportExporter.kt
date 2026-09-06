package export

import android.content.Context
import android.net.Uri
import data.model.AppSignature
import data.model.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.ByteString
import util.getUserInstalledAppInfos
import util.signaturesCompat
import util.versionCodeCompat

/**
 * 批量导出全部用户应用的签名报告(CSV,每行一个签名)。
 */
object SignatureReportExporter {

  private val csvHeader = listOf(
    "App Name",
    "Package Name",
    "Version Name",
    "Version Code",
    "Signer Index",
    "MD5",
    "SHA1",
    "SHA256",
  )

  suspend fun buildCsv(context: Context): String = withContext(Dispatchers.IO) {
    val packageManager = context.packageManager
    val rows = mutableListOf(csvHeader.joinToString(","))
    context.getUserInstalledAppInfos()
      .map { packageInfo ->
        val label = packageInfo.applicationInfo
          ?.loadLabel(packageManager)?.toString().orEmpty()
        packageInfo to label
      }
      .sortedBy { it.second.lowercase() }
      .forEach { (packageInfo, name) ->
        val versionName = csvEscape(packageInfo.versionName ?: "")
        val versionCode = packageInfo.versionCodeCompat
        packageInfo.signaturesCompat.forEachIndexed { index, signature ->
          val bytes = ByteString.of(*AppSignature.from(signature).byteArray)
          rows += listOf(
            csvEscape(name),
            csvEscape(packageInfo.packageName),
            versionName,
            versionCode.toString(),
            index.toString(),
            bytes.md5().hex(),
            bytes.sha1().hex(),
            bytes.sha256().hex(),
          ).joinToString(",")
        }
      }
    rows.joinToString("\r\n")
  }

  fun write(context: Context, uri: Uri, content: String): Boolean = runCatching {
    context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
      stream.write(content.toByteArray(Charsets.UTF_8))
    } ?: return false
    true
  }.getOrDefault(false)

  private fun csvEscape(value: String): String = if (value.contains(',') || value.contains('"') || value.contains('\n')) {
    "\"${value.replace("\"", "\"\"")}\""
  } else {
    value
  }
}

private fun okio.ByteString.hex(): String = toByteArray().joinToString("") { "%02x".format(it) }
