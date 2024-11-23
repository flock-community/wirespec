package community.flock.wirespec.integration.spring.kotlin.client

import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Serialization
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import kotlin.reflect.full.companionObjectInstance

class WirespecWebClient(
    private val client: WebClient,
    private val wirespecSerde: Serialization<String>,
) {
    suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> send(request: Req, ): Res {
        val declaringClass= request::class.java.declaringClass
        val handler = declaringClass.declaredClasses.toList()
            .find { it.simpleName == "Handler" }
            ?: error("Handler not found")
        val instance = handler.kotlin.companionObjectInstance as Wirespec.Client<Req, Res>
        return with(instance.client(wirespecSerde)) { executeRequest(to(request), client).let(::from) }
    }

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
            .bodyValue(request.body)
            .exchangeToMono { response ->
                response.bodyToMono(String::class.java)
                    .map { body ->
                        Wirespec.RawResponse(
                            statusCode = response.statusCode().value(),
                            headers = response.headers().asHttpHeaders().toSingleValueMap(),
                            body = body
                        )
                    }
            }
            .onErrorResume { throwable ->
                when (throwable) {
                    is WebClientResponseException ->
                        Wirespec.RawResponse(
                            statusCode = throwable.statusCode.value(),
                            headers = throwable.headers.toSingleValueMap(),
                            body = throwable.responseBodyAsString
                        ).let { Mono.just(it) }
                    else -> Mono.error(throwable)
                }
            }
            .awaitSingle()
}