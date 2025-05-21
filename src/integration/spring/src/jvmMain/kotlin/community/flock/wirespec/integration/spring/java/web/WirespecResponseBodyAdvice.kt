package community.flock.wirespec.integration.spring.java.web

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.java.Wirespec
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
    private val objectMapper: ObjectMapper,
    private val wirespecSerialization: Wirespec.Serialization<String>,
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
        val handler = declaringClass.declaredClasses.toList().find { it.simpleName == "Handler" }
        val handlers = handler?.declaredClasses?.toList()?.find { it.simpleName == "Handlers" } ?: error("Handlers not found")
        val instance = handlers.getDeclaredConstructor().newInstance() as Wirespec.Server<Wirespec.Request<*>, Wirespec.Response<*>>
        val server = instance.getServer(wirespecSerialization)
        return when (body) {
            is Wirespec.Response<*> -> {
                val rawResponse = server.to(body)
                response.setStatusCode(HttpStatusCode.valueOf(rawResponse.statusCode))
                rawResponse.body?.let { objectMapper.readTree(it) }
            }
            else -> body
        }
    }
}
