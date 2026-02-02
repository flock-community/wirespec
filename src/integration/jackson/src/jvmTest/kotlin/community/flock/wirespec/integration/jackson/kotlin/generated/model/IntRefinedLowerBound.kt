package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class IntRefinedLowerBound(override val value: Long): Wirespec.Refined<Long> {
  override fun toString() = value.toString()
}

fun IntRefinedLowerBound.validate() = -1 < value
