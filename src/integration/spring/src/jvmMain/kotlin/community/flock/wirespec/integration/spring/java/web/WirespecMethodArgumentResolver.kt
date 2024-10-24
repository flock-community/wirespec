package community.flock.wirespec.integration.spring.java.web


import community.flock.wirespec.integration.spring.java.ExtensionFunctions.getStaticClass
import community.flock.wirespec.integration.spring.java.ExtensionFunctions.getStaticMethode
import community.flock.wirespec.java.Wirespec
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.Collections
import java.util.function.Function
import java.util.stream.Collectors



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
        val handler = declaringClass.declaredClasses.toList().find { it.simpleName == "Handler" }
        val handlers = handler?.declaredClasses?.toList()?.find { it.simpleName == "Handlers" }
        val instance = handlers?.newInstance() as Wirespec.Server<*, *>
        val server = instance.getServer(wirespecSerialization)
        return server.from(rawRequest)
    }
}

fun HttpServletRequest.toRawRequest():Wirespec.RawRequest {
    return Wirespec.RawRequest(
        method,
        pathInfo.split("/"),
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

