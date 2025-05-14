package community.flock.wirespec.examples.openapi.app

import community.flock.wirespec.generated.kotlin.v3.endpoint.AddPet
import community.flock.wirespec.generated.kotlin.v3.endpoint.FindPetsByStatus
import community.flock.wirespec.kotlin.Wirespec
import java.net.URI
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.util.CollectionUtils.*
import org.springframework.http.RequestEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

interface KotlinPetstoreClient : AddPet.Handler, FindPetsByStatus.Handler

@Component
class LiveKotlinPetstoreClient(
    private val serialization: Wirespec.Serialization<String>,
    private val client: RestTemplate,
) : KotlinPetstoreClient {

    override suspend fun addPet(request: AddPet.Request) =
        with(AddPet.Handler.client(serialization)) {
            to(request).let(::handle).let(::from)
        }

    override suspend fun findPetsByStatus(request: FindPetsByStatus.Request) =
        with(FindPetsByStatus.Handler.client(serialization)) {
            to(request).let(::handle).let(::from)
        }

    private fun handle(request: Wirespec.RawRequest): Wirespec.RawResponse =
        RequestEntity
            .method(
                HttpMethod.valueOf(request.method),
                URI("https://6467e16be99f0ba0a819fd68.mockapi.io${request.path}"),
            )
            .headers(HttpHeaders().apply { putAll(request.headers) })
            .body(request.body ?: Unit)
            .let { client.exchange<String>(it) }
            .run { Wirespec.RawResponse(statusCode = statusCode.value(), headers = toMultiValueMap(headers), body = body) }
}
