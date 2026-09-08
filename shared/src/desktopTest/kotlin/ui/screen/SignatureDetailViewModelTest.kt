package ui.screen

import data.model.SignSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.ByteString.Companion.toByteString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignatureDetailViewModelTest {

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun detailData(signSource: SignSource) = SignatureDetailData(
    appInfo = data.model.UiAppInfo(signSource.label(), signSource.label(), 1L, "1.0", 0L, null),
    signatures = listOf(
      SignatureDetailCertificate(
        bytes = byteArrayOf(1, 2, 3).toByteString(),
        modulusHex = "ab",
        modulusString = "171",
      ),
    ),
  )

  private fun SignSource.label(): String = when (this) {
    is SignSource.PackageName -> packageName
    is SignSource.Apk -> filePath
  }

  @Test
  fun loaderResultIsPublished() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val source = SignSource.PackageName("com.example.notes")
    val viewModel = SignatureDetailViewModel(source) { detailData(it) }

    advanceUntilIdle()

    val data = viewModel.data.first { it != null }!!
    assertEquals("com.example.notes", data.appInfo.packageName)
    assertEquals(1, data.signatures.size)
    assertEquals("ab", data.signatures.first().modulusHex)
  }

  @Test
  fun unresolvableSourceKeepsNullDataLikePreviousBlankScreen() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val source = SignSource.Apk("/tmp/missing.apk")
    val viewModel = SignatureDetailViewModel(source) { null }

    advanceUntilIdle()

    assertNull(viewModel.data.value)
  }

  @Test
  fun loadIsStartedOncePerViewModel() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val loads = mutableListOf<SignSource>()
    val gate = CompletableDeferred<Unit>()
    val source = SignSource.Apk("/tmp/app.apk")
    val viewModel = SignatureDetailViewModel(source) {
      loads += it
      gate.await()
      detailData(it)
    }

    advanceUntilIdle()
    assertEquals(1, loads.size)

    gate.complete(Unit)
    advanceUntilIdle()
    assertEquals(1, loads.size)
  }
}
