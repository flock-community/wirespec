package community.flock.wirespec.examples.openapi.app

import community.flock.wirespec.generated.kotlin.v3.endpoint.AddPet
import community.flock.wirespec.generated.kotlin.v3.endpoint.FindPetsByStatus
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.util.CollectionUtils.toMultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.net.URI

interface KotlinPetstoreClient : AddPet.Handler, FindPetsByStatus.Handler

@Component
class LiveKotlinPetstoreClient(
    private val serialization: Wirespec.Serialization,
    private val client: RestTemplate,
) : KotlinPetstoreClient {

    override suspend fun addPet(request: AddPet.Request) = AddPet.Adapter.handle(request)
    override suspend fun findPetsByStatus(request: FindPetsByStatus.Request) = FindPetsByStatus.Adapter.handle(request)

    fun <Req: Wirespec.Request<*>, Res: Wirespec.Response<*>> Wirespec.Adapter<Req, Res>.handle(request: Req): Res {
        val rawRequest = toRawRequest(serialization, request)
        val rawResponse = transport(rawRequest)
        return fromRawResponse(serialization, rawResponse)
    }

    private fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse =
        RequestEntity
            .method(
                HttpMethod.valueOf(request.method),
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.path}"),
            )
            .headers(HttpHeaders().apply { putAll(request.headers) })
            .body(request.body ?: Unit)
            .let { client.exchange<ByteArray>(it) }
            .run {
                Wirespec.RawResponse(
                    statusCode = statusCode.value(),
                    headers = toMultiValueMap(headers),
                    body = body
                )
            }
}
