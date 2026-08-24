package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDto
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetTodos
data class GetTodosClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : GetTodos.Call {
  override suspend fun getTodos(done: Boolean?): GetTodos.Response<*> {
    val request = GetTodos.Request(done = done)
    val rawRequest = GetTodos.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return GetTodos.fromRawResponse(serialization, rawResponse)
  }
}
