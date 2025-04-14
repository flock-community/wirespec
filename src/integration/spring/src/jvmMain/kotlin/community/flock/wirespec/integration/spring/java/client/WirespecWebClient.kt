package community.flock.wirespec.integration.spring.java.client

import community.flock.wirespec.integration.spring.shared.filterNotEmpty
import community.flock.wirespec.java.Wirespec
import community.flock.wirespec.java.Wirespec.Serialization
import org.springframework.http.HttpMethod
import org.springframework.util.CollectionUtils.toMultiValueMap
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
    ): CompletableFuture<Res> {
        val declaringClass = request::class.java.declaringClass
        val handler = declaringClass.declaredClasses.toList()
            .find { it.simpleName == "Handler" }
            ?: error("Handler not found")

        val handlers = handler.declaredClasses.toList().find { it.simpleName == "Handlers" } ?: error("Handlers not found")
        val instance = handlers.getDeclaredConstructor().newInstance() as Wirespec.Client<Req, Res>

        return with(instance.getClient(wirespecSerde)) {
            executeRequest(to(request), client).thenApply {
                from(it)
            }
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
                .apply { request.queries.filterNotEmpty().forEach { (key, value) -> queryParam(key, value) } }
                .build()
        }
        .headers { headers ->
            request.headers.filterNotEmpty().forEach { (key, value) -> headers.addAll(key, value) }
        }
        .bodyValue(request.body)
        .exchangeToMono { response ->
            response.bodyToMono(String::class.java)
                .map { body ->
                    Wirespec.RawResponse(
                        response.statusCode().value(),
                        toMultiValueMap(response.headers().asHttpHeaders()),
                        body,
                    )
                }
        }
        .onErrorResume { throwable ->
            when (throwable) {
                is WebClientResponseException ->
                    Wirespec.RawResponse(
                        throwable.statusCode.value(),
                        toMultiValueMap(throwable.headers),
                        throwable.responseBodyAsString,
                    ).let { Mono.just(it) }

                else -> Mono.error(throwable)
            }
        }.toFuture()
}
