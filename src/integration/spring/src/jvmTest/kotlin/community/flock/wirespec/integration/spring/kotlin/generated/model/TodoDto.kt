package community.flock.wirespec.integration.spring.kotlin.generated.model
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
data class TodoDto(
  val id: TodoId,
  val name: String,
  val done: Boolean
) : Wirespec.Shape {
  override fun validate(): List<String> =
    if (!id.validate()) listOf("id") else emptyList<String>()
}
