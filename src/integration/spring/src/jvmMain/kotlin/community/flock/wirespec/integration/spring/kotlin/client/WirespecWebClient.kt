package community.flock.wirespec.integration.spring.kotlin.client

import community.flock.wirespec.integration.spring.shared.filterNotEmpty
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Serialization
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class WirespecWebClient(
    private val client: WebClient,
    private val wirespecSerde: Serialization,
) {
    private val toRequestCache = ConcurrentHashMap<Class<*>, Method>()
    private val fromResponseCache = ConcurrentHashMap<Class<*>, Method>()

    @Suppress("UNCHECKED_CAST")
    suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> send(request: Req): Res {
        val declaringClass = request::class.java.declaringClass
        val toRequest = toRequestCache.computeIfAbsent(declaringClass) { cls ->
            cls.declaredMethods.first { it.name == "toRawRequest" || it.name == "toRequest" }
        }
        val fromResponse = fromResponseCache.computeIfAbsent(declaringClass) { cls ->
            cls.declaredMethods.first { it.name == "fromRawResponse" || it.name == "fromResponse" }
        }
        val instance = declaringClass.getDeclaredField("INSTANCE").get(null)
        val rawRequest = toRequest.invoke(instance, wirespecSerde, request) as Wirespec.RawRequest
        val rawResponse = executeRequest(rawRequest, client)
        return fromResponse.invoke(instance, wirespecSerde, rawResponse) as Res
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
                        headers = response.headers().asHttpHeaders().toRawHeaders(),
                        body = body,
                    )
                }
                .switchIfEmpty(
                    Mono.just(
                        Wirespec.RawResponse(
                            statusCode = response.statusCode().value(),
                            headers = response.headers().asHttpHeaders().toRawHeaders(),
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
                        headers = throwable.headers.toRawHeaders(),
                        body = throwable.responseBodyAsByteArray,
                    ).let { Mono.just(it) }

                else -> Mono.error(throwable)
            }
        }
        .awaitSingle()

    /**
     * Converts Spring [HttpHeaders] into the [Map] expected by [Wirespec.RawResponse].
     *
     * This deliberately avoids casting [HttpHeaders] to [Map]: in Spring Framework 6 [HttpHeaders]
     * implements [org.springframework.util.MultiValueMap] (and therefore [Map]), but in Spring
     * Framework 7 it no longer does. Iterating via [HttpHeaders.forEach] works on both versions, so
     * a jar compiled against Spring 6 keeps working at runtime on Spring 7.
     */
    private fun HttpHeaders.toRawHeaders(): Map<String, List<String>> = LinkedMultiValueMap<String, String>().also { map ->
        forEach { name, values -> map[name] = values }
    }
}
