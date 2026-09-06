package com.seiko.keystoreviewer.ui.motion3

import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.metadata

/**
 * Marks a destination as participating in the activity-style motion transition.
 *
 * Attach it to a destination through [motion3Metadata]; scenes without this
 * metadata keep the plain [NavDisplay][androidx.navigation3.ui.NavDisplay]
 * transitions and never take part in the predictive-back motion.
 */
data object Motion3MetadataKey : NavMetadataKey<Boolean>

/**
 * Builds the metadata map that enables the motion3 transition for a destination.
 *
 * @param enabled pass `false` to explicitly opt a destination out.
 */
fun motion3Metadata(enabled: Boolean = true): Map<String, Any> = metadata {
  put(Motion3MetadataKey, enabled)
}
