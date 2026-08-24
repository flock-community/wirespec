package community.flock.wirespec.integration.spring.kotlin.generated
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDto
import community.flock.wirespec.integration.spring.kotlin.generated.model.TodoDtoPatch
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.RequestParrot
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.GetTodos
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.PatchTodos
import community.flock.wirespec.integration.spring.kotlin.generated.client.RequestParrotClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.GetTodosClient
import community.flock.wirespec.integration.spring.kotlin.generated.client.PatchTodosClient
data class Client(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : RequestParrot.Call, GetTodos.Call, PatchTodos.Call {
  override suspend fun requestParrot(queryParam: String?, RanDoMQueRY: String?, xRequestID: String?, RanDoMHeADer: String?, body: RequestBodyParrot): RequestParrot.Response<*> =
    RequestParrotClient(
      serialization = serialization,
      transportation = transportation
    ).requestParrot(queryParam, RanDoMQueRY, xRequestID, RanDoMHeADer, body)
  override suspend fun getTodos(done: Boolean?): GetTodos.Response<*> =
    GetTodosClient(
      serialization = serialization,
      transportation = transportation
    ).getTodos(done)
  override suspend fun patchTodos(id: String, body: TodoDtoPatch): PatchTodos.Response<*> =
    PatchTodosClient(
      serialization = serialization,
      transportation = transportation
    ).patchTodos(id, body)
}
