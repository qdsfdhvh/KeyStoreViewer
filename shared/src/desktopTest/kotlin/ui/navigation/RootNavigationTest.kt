package ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RootNavigationTest {
  @Test
  fun switchingTabsKeepsOnlyHomeAndSelectedRoot() {
    val stack = mutableListOf("apps", "tools", "keystore")
    stack.selectRoot("apps", "favorites")
    assertEquals(listOf("apps", "favorites"), stack)
  }

  @Test
  fun selectingHomeRemovesDetailsAndOtherRoots() {
    val stack = mutableListOf("apps", "history", "detail")
    stack.selectRoot("apps", "apps")
    assertEquals(listOf("apps"), stack)
  }

  @Test
  fun repeatedTabSelectionNeverAccumulatesEntries() {
    val stack = mutableListOf("apps")
    repeat(20) {
      stack.selectRoot("apps", "tools")
      stack.selectRoot("apps", "history")
      stack.selectRoot("apps", "history")
    }
    assertEquals(listOf("apps", "history"), stack)
  }

  @Test
  fun reselectingCurrentTabPopsItsDetail() {
    val stack = mutableListOf("apps", "tools", "compare")
    stack.selectRoot("apps", "tools")
    assertEquals(listOf("apps", "tools"), stack)
  }

  @Test
  fun backFromSelectedRootReturnsHome() {
    val stack = mutableListOf("apps")
    stack.selectRoot("apps", "favorites")
    stack.removeAt(stack.lastIndex)
    assertEquals(listOf("apps"), stack)
  }
}
