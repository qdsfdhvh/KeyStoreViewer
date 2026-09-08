package ui.navigation

/** Tabs return to their root; other roots retain Home as the Android back destination. */
fun <T> MutableList<T>.selectRoot(home: T, target: T) {
  val entries = if (target == home) listOf(home) else listOf(home, target)
  if (this != entries) {
    clear()
    addAll(entries)
  }
}
