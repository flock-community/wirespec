package community.flock.wirespec.integration.spring.kotlin.web

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import kotlin.reflect.full.companionObjectInstance

@ControllerAdvice
class WirespecResponseBodyAdvice(
    private val wirespecSerialization: Wirespec.Serialization,
) : ResponseBodyAdvice<Any?> {

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>?>): Boolean = Wirespec.Response::class.java.isAssignableFrom(returnType.parameterType)

    @Suppress("UNCHECKED_CAST")
    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        val declaringClass = returnType.parameterType.declaringClass
        val handler = declaringClass.declaredClasses.toList()
            .find { it.simpleName == "Handler" }
            ?: error("Handler not found")
        val instance = handler
            .kotlin.companionObjectInstance as Wirespec.Server<Wirespec.Request<*>, Wirespec.Response<*>>
        val server = instance.server(wirespecSerialization)
        return when (body) {
            is Wirespec.Response<*> -> {
                val rawResponse = server.to(body)
                response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode))
                response.headers.putAll(rawResponse.headers)
                if (rawResponse.body == null) {
                    Unit
                } else {
                    rawResponse.body?.let { RawJsonBody(String(it)) }
                }
            }

            else -> body
        }
    }
    class RawJsonBody(
        @get:JsonValue
        @get:JsonRawValue
        val json: String,
    )
}
