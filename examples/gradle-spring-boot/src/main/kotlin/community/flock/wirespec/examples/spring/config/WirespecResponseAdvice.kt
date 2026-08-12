package community.flock.wirespec.examples.spring.config

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonValue
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.companionObjectInstance

/**
 * Response body advice compatible with the IR Wirespec emitter.
 *
 * Routes the typed Response through the endpoint's `Handler.Companion` via
 * `server(serialization).to(response)` to produce the raw HTTP response.
 */
@ControllerAdvice
class WirespecResponseAdvice(
    private val serialization: Wirespec.Serialization,
) : ResponseBodyAdvice<Any?> {

    private val edgeCache = ConcurrentHashMap<Class<*>, Wirespec.ServerEdge<*, *>>()

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>?>): Boolean =
        Wirespec.Response::class.java.isAssignableFrom(returnType.parameterType)

    @Suppress("UNCHECKED_CAST")
    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        if (body !is Wirespec.Response<*>) return body
        val edge = edgeCache.computeIfAbsent(returnType.parameterType) { responseType ->
            val endpointClass = responseType.declaringClass
                ?: error("Wirespec response type ${responseType.name} has no declaring class")
            val handlerClass = endpointClass.declaredClasses.firstOrNull { it.simpleName == "Handler" }
                ?: error("Endpoint ${endpointClass.simpleName} has no Handler interface")
            val server = handlerClass.kotlin.companionObjectInstance as Wirespec.Server<*, *>
            server.server(serialization)
        } as Wirespec.ServerEdge<*, Wirespec.Response<*>>

        val rawResponse = edge.to(body)
        response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode))
        response.headers.putAll(rawResponse.headers)
        return rawResponse.body?.let { RawJsonBody(it) } ?: Unit
    }

    class RawJsonBody(
        @get:JsonValue
        @get:JsonRawValue
        val json: String,
    ) {
        constructor(bytes: ByteArray) : this(String(bytes))
    }
}
