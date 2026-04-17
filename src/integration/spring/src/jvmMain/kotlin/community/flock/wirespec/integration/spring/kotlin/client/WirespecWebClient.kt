package community.flock.wirespec.integration.spring.kotlin.client

import community.flock.wirespec.integration.spring.shared.filterNotEmpty
import community.flock.wirespec.kotlin.Wirespec
import community.flock.wirespec.kotlin.Wirespec.Serialization
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.CollectionUtils.toMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class WirespecWebClient(
    private val client: WebClient,
    private val wirespecSerde: Serialization,
) {
    private val toRequestCache = ConcurrentHashMap<Class<*>, Method>()
    private val fromResponseCache = ConcurrentHashMap<Class<*>, Method>()
    private val streamingCache = ConcurrentHashMap<Class<*>, Boolean>()
    private val streamingResponseConstructorCache = ConcurrentHashMap<Pair<Class<*>, Int>, Constructor<*>>()

    @Suppress("UNCHECKED_CAST")
    suspend fun <Req : Wirespec.Request<*>, Res : Wirespec.Response<*>> send(request: Req): Res {
        val declaringClass = request::class.java.declaringClass
        val toRequest = toRequestCache.computeIfAbsent(declaringClass) { cls ->
            cls.declaredMethods.first { it.name == "toRequest" }
        }
        val instance = declaringClass.getDeclaredField("INSTANCE").get(null)
        val rawRequest = toRequest.invoke(instance, wirespecSerde, request) as Wirespec.RawRequest
        return if (isStreaming(declaringClass)) {
            executeStreaming(rawRequest, declaringClass) as Res
        } else {
            val fromResponse = fromResponseCache.computeIfAbsent(declaringClass) { cls ->
                cls.declaredMethods.first { it.name == "fromResponse" }
            }
            val rawResponse = executeRequest(rawRequest, client)
            fromResponse.invoke(instance, wirespecSerde, rawResponse) as Res
        }
    }

    private fun isStreaming(declaringClass: Class<*>): Boolean = streamingCache.computeIfAbsent(declaringClass) { cls ->
        runCatching {
            val handlerClass = cls.declaredClasses.first { it.simpleName == "Handler" }
            val companionClass = handlerClass.declaredClasses.first { it.simpleName == "Companion" }
            companionClass.getField("STREAMING").getBoolean(null)
        }.getOrDefault(false)
    }

    private suspend fun executeStreaming(
        request: Wirespec.RawRequest,
        declaringClass: Class<*>,
    ): Wirespec.Response<*> = client
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
            response.bodyToMono(Resource::class.java)
                .map { resource -> buildStreamingResponse(declaringClass, response.statusCode().value(), resource) }
                .switchIfEmpty(
                    Mono.fromCallable {
                        buildStreamingResponse(
                            declaringClass,
                            response.statusCode().value(),
                            org.springframework.core.io.ByteArrayResource(ByteArray(0)),
                        )
                    },
                )
        }
        .onErrorResume { throwable ->
            when (throwable) {
                is WebClientResponseException -> Mono.just(
                    buildStreamingResponse(
                        declaringClass,
                        throwable.statusCode.value(),
                        org.springframework.core.io.ByteArrayResource(throwable.responseBodyAsByteArray),
                    ),
                )

                else -> Mono.error(throwable)
            }
        }
        .awaitSingle()

    private fun buildStreamingResponse(
        declaringClass: Class<*>,
        statusCode: Int,
        resource: Resource,
    ): Wirespec.Response<*> {
        val constructor = streamingResponseConstructorCache.computeIfAbsent(declaringClass to statusCode) { (cls, status) ->
            val responseClass = cls.declaredClasses.firstOrNull { it.simpleName == "Response$status" }
                ?: error("No Response$status class found in ${cls.name}")
            responseClass.declaredConstructors.first { ctor ->
                ctor.parameterCount == 1 && Resource::class.java.isAssignableFrom(ctor.parameterTypes[0])
            }
        }
        return constructor.newInstance(resource) as Wirespec.Response<*>
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
