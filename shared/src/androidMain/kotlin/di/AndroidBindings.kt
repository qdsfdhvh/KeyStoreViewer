package di

import android.content.Context
import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import ui.screen.AppListSource
import ui.screen.ControllerSignatureMonitorActions
import ui.screen.ExportReportWriter
import ui.screen.KeystoreBrowserViewModel
import ui.screen.PackageManagerAppListSource
import ui.screen.PackageManagerSignatureDetailLoader
import ui.screen.SignatureDetailLoader
import ui.screen.SignatureMonitorActions
import ui.screen.SignatureReportExportWriter

/**
 * Android platform adapters for the app graph. Each adapter captures the
 * application context only, never an Activity; the concrete `Context` binding
 * is provided by [com.seiko.keystoreviewer.di.AppGraph.Factory].
 */
@ContributesTo(AppScope::class)
@BindingContainer
object AndroidBindings {

  @Provides
  fun appListSource(context: Context): AppListSource = PackageManagerAppListSource(context)

  @Provides
  fun signatureDetailLoader(context: Context): SignatureDetailLoader = PackageManagerSignatureDetailLoader(context)

  @Provides
  fun exportReportWriter(context: Context): ExportReportWriter = SignatureReportExportWriter(context)

  @Provides
  fun signatureMonitorActions(context: Context): SignatureMonitorActions = ControllerSignatureMonitorActions(context)

  /**
   * Explicit provider (the reference pattern for interface/flag-keyed
   * construction): the ViewModel's default-argument constructor must not be
   * replaced by a graph-required binding.
   */
  @Provides
  @IntoMap
  @ViewModelKey(KeystoreBrowserViewModel::class)
  fun keystoreBrowserViewModel(): ViewModel = KeystoreBrowserViewModel()
}
