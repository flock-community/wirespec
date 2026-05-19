package community.flock.wirespec.integration.spring.kotlin.web

import community.flock.wirespec.integration.spring.shared.RawJsonBody
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.core.MethodParameter
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

@ControllerAdvice
class WirespecResponseBodyAdvice(
    private val wirespecSerialization: Wirespec.Serialization,
) : ResponseBodyAdvice<Any?> {

    private val toResponseCache = ConcurrentHashMap<Class<*>, Method>()

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
        val toResponse = toResponseCache.computeIfAbsent(declaringClass) { cls ->
            cls.declaredMethods.first { it.name == "toResponse" }
        }
        val instance = declaringClass.getDeclaredField("INSTANCE").get(null)
        return when (body) {
            is Wirespec.Response<*> -> {
                val rawResponse = toResponse.invoke(instance, wirespecSerialization, body) as Wirespec.RawResponse
                response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode))
                response.headers.putAll(rawResponse.headers)
                val responseBody = body.body
                if (responseBody is Resource) {
                    if (!response.headers.containsKey("Content-Type")) {
                        response.headers.set("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    }
                    responseBody.inputStream.use { it.copyTo(response.body) }
                    response.body.flush()
                    null
                } else if (rawResponse.body == null) {
                    Unit
                } else {
                    rawResponse.body?.let { RawJsonBody(it) }
                }
            }
            else -> body
        }
    }
}
