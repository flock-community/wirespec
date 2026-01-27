package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class IntRefinedUpperBound(override val value: Long): Wirespec.Refined<Long> {
  override fun toString() = value.toString()
}

fun IntRefinedUpperBound.validate() = value < 2
