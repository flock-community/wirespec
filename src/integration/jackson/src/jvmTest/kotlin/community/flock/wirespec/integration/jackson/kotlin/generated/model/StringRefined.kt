package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class StringRefined(override val value: String): Wirespec.Refined<String> {
  override fun toString() = value.toString()
}

fun StringRefined.validate() = true
