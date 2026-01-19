package community.flock.wirespec.integration.spring.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec

enum class OrderStatus (override val label: String): Wirespec.Enum {
  placed("placed"),
  approved("approved"),
  delivered("delivered");
  override fun toString(): String {
    return label
  }
}
