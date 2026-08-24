package community.flock.wirespec.integration.spring.kotlin.generated.model
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
data class TodoId(
  override val value: String
) : Wirespec.Refined<String> {
  override fun validate(): Boolean =
    Regex("""^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$""").matches(value)
  override fun toString(): String =
    value
}
