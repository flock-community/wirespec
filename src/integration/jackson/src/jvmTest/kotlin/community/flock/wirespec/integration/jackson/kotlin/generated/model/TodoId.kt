package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf

data class TodoId(override val value: String): Wirespec.Refined {
  override fun toString() = value
}

fun TodoId.validate() = Regex("""^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$""").matches(value)
