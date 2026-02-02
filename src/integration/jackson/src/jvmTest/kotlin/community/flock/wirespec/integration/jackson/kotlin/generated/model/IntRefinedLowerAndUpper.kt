package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class IntRefinedLowerAndUpper(override val value: Long): Wirespec.Refined<Long> {
  override fun toString() = value.toString()
}

fun IntRefinedLowerAndUpper.validate() = 3 < value && value < 4
