package community.flock.wirespec.integration.spring.kotlin.web

import community.flock.wirespec.integration.spring.shared.extractPath
import community.flock.wirespec.integration.spring.shared.extractQueries
import community.flock.wirespec.kotlin.Wirespec
import jakarta.servlet.http.HttpServletRequest
import java.util.stream.Collectors
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import kotlin.reflect.full.companionObjectInstance

class WirespecMethodArgumentResolver(
    private val wirespecSerialization: Wirespec.Serialization<String>
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        Wirespec.Request::class.java.isAssignableFrom(parameter.parameterType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Wirespec.Request<*> {
        val servletRequest = webRequest.nativeRequest as HttpServletRequest
        val declaringClass = parameter.parameterType.declaringClass
        val handler = declaringClass.declaredClasses.toList()
            .find { it.simpleName == "Handler" }
            ?: error("Handler not found")
        val instance = handler.kotlin.companionObjectInstance as Wirespec.Server<*, *>
        val server = instance.server(wirespecSerialization)
        return server.from(servletRequest.toRawRequest())
    }
}

fun HttpServletRequest.toRawRequest(): Wirespec.RawRequest = Wirespec.RawRequest(
    method = method,
    path = extractPath(),
    queries = extractQueries(),
    headers = headerNames.toList().associateWith(::getHeader),
    body = reader.lines().collect(Collectors.joining(System.lineSeparator()))
)
