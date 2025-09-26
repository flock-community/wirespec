package community.flock.wirespec.integration.spring.java.web

import community.flock.wirespec.integration.spring.shared.extractPath
import community.flock.wirespec.integration.spring.shared.extractQueries
import community.flock.wirespec.java.Wirespec
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.stream.Collectors

class WirespecMethodArgumentResolver(
    private val wirespecSerialization: Wirespec.Serialization,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean = Wirespec.Request::class.java.isAssignableFrom(parameter.parameterType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Wirespec.Request<*> {
        val servletRequest = webRequest.nativeRequest as HttpServletRequest
        val declaringClass = parameter.parameterType.declaringClass
        val handler = declaringClass.declaredClasses.toList().find { it.simpleName == "Handler" }
        val handlers = handler?.declaredClasses?.toList()?.find { it.simpleName == "Handlers" }
        val instance = handlers?.getDeclaredConstructor()?.newInstance() as Wirespec.Server<*, *>
        val server = instance.getServer(wirespecSerialization)
        return server.from(servletRequest.toRawRequest())
    }
}

fun HttpServletRequest.toRawRequest(): Wirespec.RawRequest = Wirespec.RawRequest(
    method,
    extractPath(),
    extractQueries(),
    headerNames.toList().associateWith { getHeaders(it).toList() },
    reader.lines().collect(Collectors.joining(System.lineSeparator())).toByteArray(),
)
