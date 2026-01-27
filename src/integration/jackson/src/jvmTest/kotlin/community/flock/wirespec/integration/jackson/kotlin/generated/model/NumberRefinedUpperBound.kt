package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class NumberRefinedUpperBound(override val value: Double): Wirespec.Refined<Double> {
  override fun toString() = value.toString()
}

fun NumberRefinedUpperBound.validate() = value < 2.0
