package community.flock.wirespec.integration.spring.kotlin.client

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Serialization
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class WirespecClient(
    private val client: WebClient,
    private val wirespecSerde: Serialization<String>,
) {

    suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> send(
        request: Req,
        endpoint: Wirespec.Client<Req, Res>,
    ): Res = with(endpoint.client(wirespecSerde)) { executeRequest(to(request), client).let(::from) }

    private suspend fun executeRequest(
        request: Wirespec.RawRequest,
        client: WebClient,
    ): Wirespec.RawResponse =
        client
            .method(HttpMethod.valueOf(request.method))
            .uri { uriBuilder ->
                uriBuilder
                    .path(request.path.joinToString("/"))
                    .apply { request.queries.forEach { (key, value) -> queryParam(key, value) } }
                    .build()
            }
            .headers { headers ->
                request.headers.forEach { (key, value) -> headers.add(key, value) }
            }
            .bodyValue(request.body ?: "")
            .retrieve()
            .onStatus(HttpStatusCode::isError) { response ->
                response.bodyToMono(String::class.java).map { errorBody ->
                    ErrorResponse(response.statusCode(), response.headers().asHttpHeaders(), errorBody)
                }
            }
            .toEntity(String::class.java)
            .map { response ->
                Wirespec.RawResponse(
                    statusCode = response.statusCode.value(),
                    headers = response.headers.toSingleValueMap(),
                    body = response.body
                )
            }
            .onErrorResume { throwable ->
                when (throwable) {
                    is ErrorResponse ->
                        Mono.just(
                            Wirespec.RawResponse(
                                statusCode = throwable.statusCode.value(),
                                headers = throwable.headers.toSingleValueMap(),
                                body = throwable.body
                            )
                        )

                    else -> Mono.error(throwable)
                }
            }
            .awaitSingle()

    private class ErrorResponse(
        val statusCode: HttpStatusCode,
        val headers: HttpHeaders,
        val body: String,
    ) : RuntimeException()
}
