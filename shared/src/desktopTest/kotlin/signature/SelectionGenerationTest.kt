package signature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the stale-selection guard used by the keystore browser
 * and the APK comparison screen: a late async result from an older selection
 * must be discarded instead of rendered under a newer selection.
 */
class SelectionGenerationTest {

  @Test
  fun freshSelectionInvalidatesOlderTokens() {
    val selection = SelectionGeneration()
    val tokenA = selection.invalidate()
    assertTrue(selection.isValid(tokenA))

    val tokenB = selection.invalidate()
    assertTrue("newest token stays valid", selection.isValid(tokenB))
    assertFalse("token of the replaced selection is stale", selection.isValid(tokenA))
  }

  @Test
  fun currentTokenIsStableUntilInvalidated() {
    val selection = SelectionGeneration()
    val token = selection.invalidate()
    assertEquals(token, selection.current())
    assertEquals(token, selection.current())
    selection.invalidate()
    assertTrue("old token must no longer be current", token != selection.current())
  }

  @Test
  fun parseStartedBeforeNewSelectionIsRejected() {
    // Mirrors the keystore browser sequence: parse of file A starts, then the
    // user selects file B; A's completion must be discarded.
    val selection = SelectionGeneration()
    val pickA = selection.invalidate()
    val parseToken = selection.current()

    val pickB = selection.invalidate()
    assertFalse(parseToken == pickB)
    assertTrue(selection.isValid(pickB))
    assertFalse(selection.isValid(parseToken))
    assertFalse("the old pick's read is stale too", selection.isValid(pickA))
  }
}
