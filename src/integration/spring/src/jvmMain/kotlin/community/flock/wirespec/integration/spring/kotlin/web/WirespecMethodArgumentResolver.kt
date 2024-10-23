package community.flock.wirespec.integration.spring.kotlin.web

import community.flock.wirespec.kotlin.Wirespec
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.stream.Collectors
import kotlin.reflect.full.companionObjectInstance

class WirespecMethodArgumentResolver(private val wirespecSerialization: Wirespec.Serialization<String>) :
    HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return Wirespec.Request::class.java.isAssignableFrom(parameter.parameterType)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Wirespec.Request<*> {
        val servletRequest = webRequest.nativeRequest as HttpServletRequest
        val rawRequest = servletRequest.toRawRequest();
        val declaringClass = parameter.parameterType.declaringClass
        val handler =
            declaringClass.declaredClasses.toList().find { it.simpleName == "Handler" } ?: error("Handler not found")
        println("+++++++++++++++++")
        println(handler)
        println("+++++++++++++++++")
        val instance = handler.kotlin.companionObjectInstance as Wirespec.Server<*, *>
        val server = instance.server(wirespecSerialization)
        return server.from(rawRequest)
    }

}

fun HttpServletRequest.toRawRequest(): Wirespec.RawRequest {
    return Wirespec.RawRequest(
        method,
        pathInfo.split("/").drop(1),
        queryString
            ?.split("&")
            ?.associate {
                val (key, value) = it.split("=")
                key to value
            }
            .orEmpty(),
        getHeaderNames().toList().map { it to getHeader(it) }.toMap(),
        getReader().lines().collect(Collectors.joining(System.lineSeparator()))
    )
}
