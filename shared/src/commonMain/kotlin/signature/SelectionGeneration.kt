package signature

/**
 * Tracks the newest selection so late asynchronous results from an older
 * selection can be discarded instead of being rendered against newer UI state
 * (e.g. a slow keystore parse must never display its result under a file that
 * was selected afterwards).
 *
 * Single-threaded (Compose UI thread) by design.
 */
class SelectionGeneration {
  private var current = 0

  /** The token identifying the current selection. */
  fun current(): Int = current

  /**
   * Invalidate every previously issued token (a new selection arrived) and
   * return the token for the new selection.
   */
  fun invalidate(): Int = ++current

  /** True only if [token] still identifies the current selection. */
  fun isValid(token: Int): Boolean = token == current
}
