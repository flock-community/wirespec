package community.flock.wirespec.examples.spring.config

import community.flock.wirespec.kotlin.Wirespec
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.companionObjectInstance

/**
 * Argument resolver compatible with the IR Wirespec emitter.
 *
 * Reaches the endpoint's `Handler.Companion` (which implements `Wirespec.Server`)
 * via reflection and uses `server(serialization).from(rawRequest)` to build the typed Request.
 */
class WirespecRequestArgumentResolver(
    private val serialization: Wirespec.Serialization,
) : HandlerMethodArgumentResolver {

    private val edgeCache = ConcurrentHashMap<Class<*>, Wirespec.ServerEdge<*, *>>()

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        Wirespec.Request::class.java.isAssignableFrom(parameter.parameterType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Wirespec.Request<*> {
        val servletRequest = webRequest.nativeRequest as HttpServletRequest
        val edge = edgeCache.computeIfAbsent(parameter.parameterType) { requestType ->
            val endpointClass = requestType.declaringClass
                ?: error("Wirespec request type ${requestType.name} has no declaring class")
            val handlerClass = endpointClass.declaredClasses.firstOrNull { it.simpleName == "Handler" }
                ?: error("Endpoint ${endpointClass.simpleName} has no Handler interface")
            val server = handlerClass.kotlin.companionObjectInstance as Wirespec.Server<*, *>
            server.server(serialization)
        }
        return edge.from(servletRequest.toRawRequest())
    }

    private fun HttpServletRequest.toRawRequest(): Wirespec.RawRequest = Wirespec.RawRequest(
        method = method,
        path = (pathInfo ?: servletPath).split("/").filter { it.isNotEmpty() },
        queries = parseQueries(queryString),
        headers = headerNames.toList().associateWith { name -> getHeaders(name).toList() },
        body = inputStream.readAllBytes().takeIf { it.isNotEmpty() },
    )

    private fun parseQueries(queryString: String?): Map<String, List<String>> = queryString
        ?.split("&")
        ?.filter { it.isNotEmpty() }
        ?.map {
            val (key, value) = it.split("=", limit = 2).let { parts ->
                if (parts.size == 1) parts[0] to "" else parts[0] to parts[1]
            }
            java.net.URLDecoder.decode(key, Charsets.UTF_8) to
                java.net.URLDecoder.decode(value, Charsets.UTF_8)
        }
        ?.groupBy({ it.first }, { it.second })
        .orEmpty()
}
