package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class NumberRefinedUpperound(override val value: Double): Wirespec.Refined<Double> {
  override fun toString() = value.toString()
}

fun NumberRefinedUpperound.validate() = value < 2.0
