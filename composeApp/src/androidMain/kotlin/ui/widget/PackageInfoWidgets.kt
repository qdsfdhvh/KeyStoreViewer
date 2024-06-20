package ui.widget

import android.content.Context
import android.content.pm.PackageInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmapOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberPackageName(
    packageInfo: PackageInfo,
    context: Context = LocalContext.current,
): String {
    return rememberSaveable {
        packageInfo.applicationInfo.loadLabel(context.packageManager).toString()
    }
}

@Composable
fun rememberPackageIcon(
    packageInfo: PackageInfo,
    context: Context = LocalContext.current,
): State<ImageBitmap?> {
    return produceState<ImageBitmap?>(null, packageInfo, context) {
        value = withContext(Dispatchers.IO) {
            packageInfo.applicationInfo.loadIcon(context.packageManager)
                ?.toBitmapOrNull()
                ?.asImageBitmap()
        }
    }
}
