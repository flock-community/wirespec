package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class NumberRefinedLowerAndUpper(override val value: Double): Wirespec.Refined<Double> {
  override fun toString() = value.toString()
}

fun NumberRefinedLowerAndUpper.validate() = 3.0 < value && value < 4.0
