package signature

/**
 * Order-independent comparison of two complete "current signer" SHA-256 sets.
 *
 * Empty sets are a distinct invalid case on each side and are NEVER treated as
 * equal signers: two unread/unsigned inputs must produce an error outcome, not
 * a match.
 */
data class SignerSetComparison(
  val outcome: SignerSetComparisonOutcome,
  val onlyInLeft: Set<String> = emptySet(),
  val onlyInRight: Set<String> = emptySet(),
)

enum class SignerSetComparisonOutcome {
  IDENTICAL,
  DIFFERENT,
  LEFT_EMPTY,
  RIGHT_EMPTY,
  BOTH_EMPTY,
}

fun compareSignerSets(
  left: Set<String>,
  right: Set<String>,
): SignerSetComparison = when {
  left.isEmpty() && right.isEmpty() -> SignerSetComparison(SignerSetComparisonOutcome.BOTH_EMPTY)

  left.isEmpty() -> SignerSetComparison(SignerSetComparisonOutcome.LEFT_EMPTY)

  right.isEmpty() -> SignerSetComparison(SignerSetComparisonOutcome.RIGHT_EMPTY)

  left == right -> SignerSetComparison(SignerSetComparisonOutcome.IDENTICAL)

  else -> SignerSetComparison(
    outcome = SignerSetComparisonOutcome.DIFFERENT,
    onlyInLeft = left - right,
    onlyInRight = right - left,
  )
}
