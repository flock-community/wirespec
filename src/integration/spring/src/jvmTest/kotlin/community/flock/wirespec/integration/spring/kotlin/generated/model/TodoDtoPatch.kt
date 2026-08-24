package community.flock.wirespec.integration.spring.kotlin.generated.model
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
data class TodoDtoPatch(
  val name: String?,
  val done: Boolean?
) : Wirespec.Shape {
  override fun validate(): List<String> =
    emptyList<String>()
}
