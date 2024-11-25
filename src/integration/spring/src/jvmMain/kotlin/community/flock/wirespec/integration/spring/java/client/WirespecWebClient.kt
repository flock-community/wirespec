package community.flock.wirespec.integration.spring.java.client

import community.flock.wirespec.java.Wirespec
import community.flock.wirespec.java.Wirespec.Serialization
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import kotlin.reflect.full.companionObjectInstance

class WirespecWebClient(
    private val client: WebClient,
    private val wirespecSerde: Serialization<String>,
) {
    fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> send(
        request: Req,
    ): CompletableFuture<Res> {
        val declaringClass= request::class.java.declaringClass
        val handler = declaringClass.declaredClasses.toList()
            .find { it.simpleName == "Handler" }
            ?: error("Handler not found")

        val instance = handler.kotlin.companionObjectInstance as Wirespec.Client<Req, Res>

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