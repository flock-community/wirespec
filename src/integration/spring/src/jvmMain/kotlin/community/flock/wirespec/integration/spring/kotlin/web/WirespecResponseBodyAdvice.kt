package community.flock.wirespec.integration.spring.kotlin.web

import community.flock.wirespec.integration.spring.shared.RawJsonBody
import community.flock.wirespec.integration.spring.shared.findAdapter
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

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
        val adapter = returnType.parameterType.declaringClass.findAdapter<Wirespec.Request<*>, Wirespec.Response<*>>()
        return when (body) {
            is Wirespec.Response<*> -> {
                val rawResponse = adapter.toRawResponse(wirespecSerialization, body) as Wirespec.RawResponse
                response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode))
                response.headers.putAll(rawResponse.headers)
                if (rawResponse.body == null) {
                    Unit
                } else {
                    rawResponse.body?.let { RawJsonBody(it) }
                }
            }
            else -> body
        }
    }
}
