package community.flock.wirespec.integration.spring.java.web

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
class WirespecResponseBodyAdvice : ResponseBodyAdvice<Any?> {
    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>?>): Boolean {
        return Wirespec.Response::class.java.isAssignableFrom(returnType.parameterType)
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        return when (body) {
            is Wirespec.Response<*> -> {
                response.setStatusCode(HttpStatusCode.valueOf(body.status))
                body.body
            }

            else -> body
        }
    }
}