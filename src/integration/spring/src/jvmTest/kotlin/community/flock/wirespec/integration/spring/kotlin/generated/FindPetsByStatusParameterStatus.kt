package community.flock.wirespec.integration.spring.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

enum class FindPetsByStatusParameterStatus (override val label: String): Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  override fun toString(): String {
    return label
  }
}
