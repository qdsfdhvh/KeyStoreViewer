package signature

import kotlinx.coroutines.CancellationException
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Regression tests for the bounded, cancellation-aware APK temp copy used by
 * the comparison screen ([signature.copyBounded]).
 */
class BoundedCopyTest {

  @Test
  fun copiesExactBytes() {
    val data = ByteArray(256 * 1024) { (it % 251).toByte() }
    val source = Buffer().write(data)
    val sink = Buffer()
    copyBounded(source, sink)
    assertEquals(256 * 1024L, sink.size)
    assertTrue(
      "copied bytes must match the source",
      data.contentEquals(sink.readByteArray()),
    )
  }

  @Test
  fun emptySourceCopiesNothing() {
    val sink = Buffer()
    copyBounded(Buffer(), sink)
    assertEquals(0L, sink.size)
  }

  @Test
  fun exceedingTheLimitAbortsTheCopy() {
    val source = Buffer().write(ByteArray(300 * 1024))
    val sink = Buffer()
    try {
      copyBounded(source, sink, maxBytes = 128 * 1024L)
      fail("expected ApkTooLargeException")
    } catch (expected: ApkTooLargeException) {
      assertEquals(128 * 1024L, expected.maxBytes)
    }
    assertTrue(
      "copy must abort at the limit, not copy everything",
      sink.size in 1..128 * 1024L + 64 * 1024L,
    )
  }

  @Test
  fun cancellationCheckStopsTheCopy() {
    val source = Buffer().write(ByteArray(10 * 1024 * 1024))
    val sink = Buffer()
    var checks = 0
    try {
      copyBounded(source, sink, maxBytes = Long.MAX_VALUE) {
        checks++
        if (checks >= 3) {
          throw CancellationException("left the screen")
        }
      }
      fail("expected cancellation")
    } catch (expected: CancellationException) {
      assertEquals("left the screen", expected.message)
    }
    assertTrue(
      "cancelled copy must have stopped early",
      sink.size < 10 * 1024 * 1024L,
    )
  }
}
