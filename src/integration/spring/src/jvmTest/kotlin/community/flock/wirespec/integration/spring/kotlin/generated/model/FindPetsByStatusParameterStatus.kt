package community.flock.wirespec.integration.spring.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import io.ktor.util.CaseInsensitiveMap
import io.ktor.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf

enum class FindPetsByStatusParameterStatus (override val label: String): Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  override fun toString(): String {
    return label
  }
}
