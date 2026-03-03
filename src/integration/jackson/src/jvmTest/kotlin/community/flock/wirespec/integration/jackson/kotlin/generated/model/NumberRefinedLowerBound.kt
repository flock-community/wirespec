package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class NumberRefinedLowerBound(override val value: Double): Wirespec.Refined<Double> {
  override fun toString() = value.toString()
  override fun validate() = -1.0 < value
}
