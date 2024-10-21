package community.flock.wirespec.integration.spring.java.web


import community.flock.wirespec.java.Wirespec
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

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
    ): Any? {
        TODO("Not yet implemented")
    }

//    override fun resolveArgument(
//        parameter: MethodParameter,
//        mavContainer: ModelAndViewContainer?,
//        webRequest: NativeWebRequest,
//        binderFactory: WebDataBinderFactory?
//    ): Wirespec.Request<*> {
//        val request = webRequest.nativeRequest as HttpServletRequest
//        val static = parameter.parameterType.declaringClass
//        val requestMapper = static.getStaticMethode("REQUEST_MAPPER") ?: error("request mapper not found")
//        val wirespecContent = request.contentType?.let { Wirespec.Content(it, request.reader) }
//        val wirespecRequest = object : Wirespec.Request<BufferedReader> {
//            override val path = request.requestURI
//            override val method = Wirespec.Method.valueOf(request.method)
//            override val query = request.parameterMap.mapValues { it.value.toList() }
//            override val headers = request.headerNames.toList().associateWith { request.getHeaders(it).toList() }
//            override val content = wirespecContent
//
//        }
//        return static.invoke(requestMapper, contentMapper, wirespecRequest)
//    }

}

//fun Wirespec.Request<*>.parsePathParams(): Map<String, String> {
//    val comp = this.javaClass.declaringClass.kotlin.companionObjectInstance
//    val path = comp?.javaClass?.getDeclaredField("PATH")?.get(comp) as String? ?: error("cannot find path")
//    val parser = PathPatternParser()
//    val result = parser.parse(path)
//    val info = result.matchAndExtract(PathContainer.parsePath(this.path))
//    return info?.uriVariables ?: emptyMap()
//}
