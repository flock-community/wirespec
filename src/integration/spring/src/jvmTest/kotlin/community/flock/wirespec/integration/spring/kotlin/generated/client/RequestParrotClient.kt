package community.flock.wirespec.integration.spring.kotlin.generated.client
import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.typeOf
import community.flock.wirespec.integration.spring.kotlin.generated.model.RequestBodyParrot
import community.flock.wirespec.integration.spring.kotlin.generated.model.Error
import community.flock.wirespec.integration.spring.kotlin.generated.endpoint.RequestParrot
data class RequestParrotClient(
  val serialization: Wirespec.Serialization,
  val transportation: Wirespec.Transportation
) : RequestParrot.Call {
  override suspend fun requestParrot(queryParam: String?, RanDoMQueRY: String?, xRequestID: String?, RanDoMHeADer: String?, body: RequestBodyParrot): RequestParrot.Response<*> {
    val request = RequestParrot.Request(
      queryParam = queryParam,
      RanDoMQueRY = RanDoMQueRY,
      xRequestID = xRequestID,
      RanDoMHeADer = RanDoMHeADer,
      body = body
    )
    val rawRequest = RequestParrot.toRawRequest(serialization, request)
    val rawResponse = transportation.transport(rawRequest)
    return RequestParrot.fromRawResponse(serialization, rawResponse)
  }
}
