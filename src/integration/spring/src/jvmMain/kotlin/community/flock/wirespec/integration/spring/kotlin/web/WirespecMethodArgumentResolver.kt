package community.flock.wirespec.integration.spring.kotlin.web

import community.flock.wirespec.kotlin.Wirespec
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
//        val requestMapper = static.kotlin.companionObjectInstance as Wirespec.H
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
