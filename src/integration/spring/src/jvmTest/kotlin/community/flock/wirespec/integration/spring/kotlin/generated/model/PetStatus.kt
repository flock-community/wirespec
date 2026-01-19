package community.flock.wirespec.integration.spring.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec

enum class PetStatus (override val label: String): Wirespec.Enum {
  available("available"),
  pending("pending"),
  sold("sold");
  override fun toString(): String {
    return label
  }
}
