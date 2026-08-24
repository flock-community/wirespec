package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDtoPatch
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDto
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.PatchTodos
data class PatchTodosClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : PatchTodos.Call {
  override suspend fun patchTodos(id: String, body: TodoDtoPatch): PatchTodos.Response<*> {
    val request = PatchTodos.Request(
      id = id,
      body = body
    )
    val rawRequest = PatchTodos.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return PatchTodos.fromRawResponse(serialization, rawResponse)
  }
}
