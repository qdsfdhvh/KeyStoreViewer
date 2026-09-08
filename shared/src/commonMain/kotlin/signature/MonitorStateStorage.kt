package signature

/**
 * Persistence seam for the signature-change monitor state.
 *
 * Contract:
 * - Each [transact] is an atomic read-modify-write cycle: the transform sees
 *   the latest committed state and the resulting state is durably stored
 *   before [transact] returns.
 * - Failures PROPAGATE: an implementation must never silently swallow a
 *   storage error, because scan/enable/disable success may only be reported
 *   after a durable state change.
 */
interface MonitorStateStorage {
  suspend fun load(): MonitorState

  /**
   * Atomically apply [block] to the committed state, persist the returned
   * state and return the second element of the pair produced by [block].
   * Throws when the state cannot be stored.
   */
  suspend fun <T> transact(block: (MonitorState) -> Pair<MonitorState, T>): T

  /** Remove all stored state (used when the monitor is disabled). */
  suspend fun deleteAll()
}
