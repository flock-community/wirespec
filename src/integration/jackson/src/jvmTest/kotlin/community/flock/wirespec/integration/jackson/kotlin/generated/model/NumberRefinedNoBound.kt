package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class NumberRefinedNoBound(override val value: Double): Wirespec.Refined<Double> {
  override fun toString() = value.toString()
  override fun validate() = true
}
