package community.flock.wirespec.integration.spring.kotlin.client

import community.flock.wirespec.integration.spring.shared.filterNotEmpty
import community.flock.wirespec.integration.spring.shared.findAdapter
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Serialization
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.CollectionUtils.toMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class WirespecWebClient(
    private val client: WebClient,
    private val wirespecSerde: Serialization,
) {

    fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> Req.findAdapter(): Wirespec.Adapter<Req, Res> {
        val endpointClass = this::class.java.declaringClass
        val adapterClass = endpointClass.declaredClasses.toList().find { Wirespec.Adapter::class.java.isAssignableFrom(it) } ?: error("Adapter not found")
        return adapterClass.kotlin.objectInstance as? Wirespec.Adapter<Req, Res> ?: error("Adapter not initialized")
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> send(request: Req): Res {
        val adapter = request::class.java.declaringClass.findAdapter<Req, Res>()
        val rawRequest = adapter.toRawRequest(wirespecSerde, request)
        val rawResponse: Wirespec.RawResponse = executeRequest(rawRequest, client)
        return adapter.fromRawResponse(wirespecSerde, rawResponse)
    }

    private suspend fun executeRequest(
        request: Wirespec.RawRequest,
        client: WebClient,
    ): Wirespec.RawResponse = client
        .method(HttpMethod.valueOf(request.method))
        .uri { uriBuilder ->
            uriBuilder
                .path(request.path.joinToString("/"))
                .apply { request.queries.filterNotEmpty().forEach { (key, value) -> queryParam(key, value) } }
                .build()
        }
        .headers { headers ->
            request.headers.filterNotEmpty().forEach { (key, value) -> headers.addAll(key, value) }
        }
        .apply {
            request.body?.let {
                contentType(MediaType.APPLICATION_JSON)
                bodyValue(it)
            }
        }
        .exchangeToMono { response ->
            response.bodyToMono(ByteArray::class.java)
                .map { body ->
                    Wirespec.RawResponse(
                        statusCode = response.statusCode().value(),
                        headers = toMultiValueMap(response.headers().asHttpHeaders()),
                        body = body,
                    )
                }
                .switchIfEmpty(
                    Mono.just(
                        Wirespec.RawResponse(
                            statusCode = response.statusCode().value(),
                            headers = toMultiValueMap(response.headers().asHttpHeaders()),
                            body = null,
                        ),
                    ),
                )
        }
        .onErrorResume { throwable ->
            when (throwable) {
                is WebClientResponseException ->
                    Wirespec.RawResponse(
                        statusCode = throwable.statusCode.value(),
                        headers = toMultiValueMap(throwable.headers),
                        body = throwable.responseBodyAsByteArray,
                    ).let { Mono.just(it) }

                else -> Mono.error(throwable)
            }
        }
        .awaitSingle()
}
