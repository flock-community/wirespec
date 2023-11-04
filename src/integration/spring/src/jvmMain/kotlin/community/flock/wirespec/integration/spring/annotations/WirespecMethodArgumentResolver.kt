package community.flock.wirespec.integration.spring.annotations


import community.flock.wirespec.Wirespec
import community.flock.wirespec.integration.spring.annotations.Util.invokeStatic
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.server.PathContainer
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.util.pattern.PathPatternParser
import java.io.BufferedReader
import kotlin.reflect.full.companionObjectInstance

typealias RequestMapper = (path:String, method: Wirespec.Method, query: Map<String, List<Any?>>, headers:Map<String, List<Any?>>, content: Wirespec.Content<BufferedReader>?) -> Wirespec.Request<*>
class WirespecMethodArgumentResolver(private val contentMapper: JacksonContentMapper): HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return Wirespec.Request::class.java.isAssignableFrom(parameter.parameterType)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Wirespec.Request<*> {
        val request = webRequest.nativeRequest as HttpServletRequest

        val static = parameter.parameterType.declaringClass
        val requestMapper = static.javaClass.methods.find { it.name == "REQUEST_MAPPER" }
        val content = request.contentType?.let {  Wirespec.Content(it, request.reader) }

        return requestMapper?.invokeStatic(
            static,
            contentMapper,
            request.requestURI,
            Wirespec.Method.valueOf(request.method),
            request.parameterMap.mapValues { it.value.toList() },
            request.headerNames.toList().associateWith { request.getHeaders(it).toList() },
            content as Any
        )?: error("")
    }
}

fun Wirespec.Request<*>.parsePathParams(): Map<String, String> {
    val comp = this.javaClass.declaringClass.kotlin.companionObjectInstance
    val path = comp?.javaClass?.getDeclaredField("PATH")?.get(comp) as String? ?: error("cannot find path")
    val parser = PathPatternParser()
    val result = parser.parse(path)
    val info = result.matchAndExtract(PathContainer.parsePath(this.path))
    return info?.uriVariables ?: emptyMap()
}
