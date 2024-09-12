package community.flock.wirespec.integration.jackson.kotlin.generated

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class TodoId(override val value: String): Wirespec.Refined {
  override fun toString() = value
}

fun TodoId.validate() = Regex("""^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}$""").matches(value)
data class Todo(
  val id: TodoId,
  val name: String,
  val final: Boolean,
  val category: TodoCategory
)
data class TodoInput(
  val name: String,
  val done: Boolean
)
data class Error(
  val code: String,
  val description: String
)
enum class TodoCategory (val label: String): Wirespec.Enum {
  WORK("WORK"),
  LIFE("LIFE");
  override fun toString(): String {
    return label
  }
}
