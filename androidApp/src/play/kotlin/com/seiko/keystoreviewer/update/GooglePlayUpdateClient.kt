package com.seiko.keystoreviewer.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

internal class GooglePlayUpdateClient(
  context: Context,
  private val launcher: ActivityResultLauncher<IntentSenderRequest>,
) : UpdateClient {
  private val manager = AppUpdateManagerFactory.create(context.applicationContext)
  private var listener: InstallStateUpdatedListener? = null

  override fun observe(listener: (UpdateStatus) -> Unit) {
    stopObserving()
    val observer = InstallStateUpdatedListener { listener(it.installStatus().toUpdateStatus()) }
    this.listener = observer
    manager.registerListener(observer)
  }

  override fun stopObserving() {
    listener?.let(manager::unregisterListener)
    listener = null
  }

  override fun check(result: (Result<UpdateOffer>) -> Unit) {
    runCatching {
      manager.appUpdateInfo
        .addOnSuccessListener { info ->
          val launch = if (
            info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
          ) {
            {
              manager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
              )
            }
          } else {
            null
          }
          result(Result.success(UpdateOffer(info.installStatus().toUpdateStatus(), launch)))
        }
        .addOnFailureListener { result(Result.failure(it)) }
    }.onFailure { result(Result.failure(it)) }
  }

  override fun complete(result: (Result<Unit>) -> Unit) {
    runCatching {
      manager.completeUpdate()
        .addOnSuccessListener { result(Result.success(Unit)) }
        .addOnFailureListener { result(Result.failure(it)) }
    }.onFailure { result(Result.failure(it)) }
  }
}

private fun Int.toUpdateStatus(): UpdateStatus = when (this) {
  InstallStatus.PENDING -> UpdateStatus.Pending
  InstallStatus.DOWNLOADING -> UpdateStatus.Downloading
  InstallStatus.DOWNLOADED -> UpdateStatus.Downloaded
  InstallStatus.INSTALLING -> UpdateStatus.Installing
  InstallStatus.INSTALLED -> UpdateStatus.Installed
  InstallStatus.FAILED -> UpdateStatus.Failed
  InstallStatus.CANCELED -> UpdateStatus.Canceled
  else -> UpdateStatus.Idle
}
