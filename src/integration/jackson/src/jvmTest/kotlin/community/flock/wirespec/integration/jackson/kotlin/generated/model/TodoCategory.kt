package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap
import community.flock.wirespec.kotlin.util.CaseInsensitiveMap.Companion.toCaseInsensitive
import kotlin.reflect.typeOf

enum class TodoCategory (override val label: String): Wirespec.Enum {
  WORK("WORK"),
  LIFE("LIFE");
  override fun toString(): String {
    return label
  }
}
