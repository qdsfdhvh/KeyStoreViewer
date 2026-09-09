package com.seiko.keystoreviewer.di

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.lifecycle.viewmodel.CreationExtras
import data.model.SignSource
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import ui.screen.FavoritesViewModel
import ui.screen.HistoryViewModel
import ui.screen.SignatureDetailViewModel
import java.io.File
import java.nio.file.Files

/**
 * Exercises the generated [AppGraph] end to end on the JVM: app-scoped
 * singleton provider identity, per-instance ViewModel construction through the
 * Metro-generated factory, and the assisted factory carrying the runtime
 * [SignSource]. The graph is the production one; only the Context backing the
 * file/preferences adapters is faked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    // ViewModels launch their init work on viewModelScope (Main.immediate).
    // Queuing it on a never-advanced test scheduler keeps the assertions on
    // graph wiring and instance identity; no loader/framework code runs.
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun graph(): AppGraph = createGraphFactory<AppGraph.Factory>().create(FakeAppContext())

  @Test
  fun appScopedBindingsKeepSingletonIdentity() {
    val graph = graph()

    assertSame(graph.historyRepository, graph.historyRepository)
    assertSame(graph.favoritesRepository, graph.favoritesRepository)
    assertSame(graph.exportQuota, graph.exportQuota)
    assertSame(graph.metroViewModelFactory, graph.metroViewModelFactory)
  }

  @Test
  fun viewModelFactoryBuildsDistinctEntryScopedOwners() {
    val factory = graph().metroViewModelFactory

    val historyA = factory.create(HistoryViewModel::class, CreationExtras.Empty)
    val historyB = factory.create(HistoryViewModel::class, CreationExtras.Empty)
    assertNotSame(historyA, historyB)
    assertEquals(HistoryViewModel::class, historyA::class)

    val favoritesA = factory.create(FavoritesViewModel::class, CreationExtras.Empty)
    val favoritesB = factory.create(FavoritesViewModel::class, CreationExtras.Empty)
    assertNotSame(favoritesA, favoritesB)
    assertEquals(FavoritesViewModel::class, favoritesA::class)
  }

  @Test
  fun assistedFactoryCarriesRuntimeSignSourceIntoDistinctViewModels() {
    val factory = graph().metroViewModelFactory

    val apkSource = SignSource.Apk("/tmp/a.apk")
    val packageSource = SignSource.PackageName("com.example")
    // The generated factory is a stateless singleton; each create() call must
    // produce a distinct ViewModel carrying exactly the requested source.
    val factoryA = factory.createManuallyAssistedFactory(SignatureDetailViewModel.Factory::class)()
    val factoryB = factory.createManuallyAssistedFactory(SignatureDetailViewModel.Factory::class)()

    val vmA = factoryA.create(apkSource)
    val vmB = factoryB.create(packageSource)
    assertNotSame(vmA, vmB)
    assertEquals(apkSource, vmA.signSource)
    assertEquals(packageSource, vmB.signSource)
  }
}

/**
 * Plain-JVM application context: file and SharedPreferences access are
 * redirected to a temp directory / in-memory store so the graph's adapters
 * (file repositories, quota store) run without the Android framework.
 */
private class FakeAppContext : ContextWrapper(null) {

  private val filesDir: File = Files.createTempDirectory("ksv-graph-test").toFile()
  private val preferences: SharedPreferences = InMemorySharedPreferences()

  override fun getFilesDir(): File = filesDir

  override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = preferences

  override fun getApplicationContext(): Context = this
}

/** Minimal in-memory [SharedPreferences]: enough for the quota store reads/writes. */
private class InMemorySharedPreferences : SharedPreferences {

  private val values = mutableMapOf<String, Any>()

  override fun getAll(): MutableMap<String, *> = values.toMutableMap()

  override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

  override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue

  override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue

  override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue

  override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

  override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = @Suppress("UNCHECKED_CAST")
  values[key] as? MutableSet<String> ?: defValues

  override fun contains(key: String): Boolean = values.containsKey(key)

  override fun edit(): SharedPreferences.Editor = Editor()

  override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

  override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

  private inner class Editor : SharedPreferences.Editor {

    private val pending = mutableMapOf<String, Any?>()

    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply { pending[key] = value }

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply { pending[key] = value }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply { pending[key] = value }

    override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply { pending[key] = value }

    override fun putString(key: String, value: String?): SharedPreferences.Editor = apply { pending[key] = value }

    override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
      pending[key] = values
    }

    override fun remove(key: String): SharedPreferences.Editor = apply { pending[key] = null }

    override fun clear(): SharedPreferences.Editor = apply { values.clear() }

    override fun apply() {
      pending.forEach { (key, value) ->
        if (value == null) values.remove(key) else values[key] = value
      }
      pending.clear()
    }

    override fun commit(): Boolean {
      apply()
      return true
    }
  }
}
