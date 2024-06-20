package ui.component.navigator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

class Navigator(
  initialScreen: Screen,
  initialIndex: Int = 0,
) {
  private var index = initialIndex
  private val screens = mutableStateListOf(initialScreen.toEntry())

  val lastItem: BackStackEntry get() = screens.last()

  var lastEvent by mutableStateOf(LastEvent.Push)
    private set

  private val canPop: Boolean get() = screens.size > 1

  fun push(screen: Screen) {
    lastEvent = LastEvent.Push
    screens.add(screen.toEntry())
  }

  fun pop(): Boolean {
    if (canPop) {
      lastEvent = LastEvent.Pop
      screens.removeLast()
      return true
    }
    return false
  }

  private fun Screen.toEntry() = BackStackEntry(
    index = index++,
    screen = this,
  )
}

class BackStackEntry(
  val index: Int,
  val screen: Screen,
)

enum class LastEvent {
  Push,
  Pop,
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Not provide a Navigator") }
