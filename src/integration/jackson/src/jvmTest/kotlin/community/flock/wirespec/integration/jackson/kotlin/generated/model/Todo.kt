package community.flock.wirespec.integration.jackson.kotlin.generated.model

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf

data class Todo(
  val id: TodoId,
  val name: String,
  val final: Boolean,
  val category: TodoCategory,
  val eMail: String
)
