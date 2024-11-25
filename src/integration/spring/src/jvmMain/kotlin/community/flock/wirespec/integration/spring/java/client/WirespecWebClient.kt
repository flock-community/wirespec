package community.flock.wirespec.integration.spring.java.client

import community.flock.wirespec.java.Wirespec
import community.flock.wirespec.java.Wirespec.Serialization
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

class WirespecWebClient(
    private val client: WebClient,
    private val wirespecSerde: Serialization<String>,
) {
    fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> send(
        request: Req,
        endpoint: Wirespec.Client<Req, Res>,
    ): CompletableFuture<Res> {

        val clientEdge = endpoint.getClient(wirespecSerde)
        val rawRequest = clientEdge.to(request)

        return executeRequest(rawRequest, client).thenApply {
            clientEdge.from(it)
        }
    }

    private fun executeRequest(
        request: Wirespec.RawRequest,
        client: WebClient,
    ): CompletableFuture<Wirespec.RawResponse> = client
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
        .bodyValue(request.body)
        .exchangeToMono { response ->
            response.bodyToMono(String::class.java)
                .map { body ->
                    Wirespec.RawResponse(
                        response.statusCode().value(),
                        response.headers().asHttpHeaders().toSingleValueMap(),
                        body
                    )
                }
        }
        .onErrorResume { throwable ->
            when (throwable) {
                is WebClientResponseException ->
                    Wirespec.RawResponse(
                        throwable.statusCode.value(),
                        throwable.headers.toSingleValueMap(),
                        throwable.responseBodyAsString
                    ).let { Mono.just(it) }

                else -> Mono.error(throwable)
            }
        }.toFuture()

}