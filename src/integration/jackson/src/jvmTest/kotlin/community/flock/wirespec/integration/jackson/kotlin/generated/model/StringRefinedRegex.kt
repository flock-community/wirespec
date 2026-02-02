package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class StringRefinedRegex(override val value: String): Wirespec.Refined<String> {
  override fun toString() = value.toString()
}

fun StringRefinedRegex.validate() = Regex("""^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$""").matches(value)
