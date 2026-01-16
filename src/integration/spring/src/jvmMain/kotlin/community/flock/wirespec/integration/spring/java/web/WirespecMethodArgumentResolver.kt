package community.flock.wirespec.integration.spring.java.web

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.spring.shared.extractPath
import community.flock.wirespec.integration.spring.shared.extractQueries
import community.flock.wirespec.java.Wirespec
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.multipart.MultipartHttpServletRequest
import kotlin.io.encoding.ExperimentalEncodingApi

class WirespecMethodArgumentResolver(
    private val objectMapper: ObjectMapper,
    private val wirespecSerialization: Wirespec.Serialization,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        Wirespec.Request::class.java.isAssignableFrom(parameter.parameterType)

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
        val req = servletRequest.toRawRequest()
        return instance.getServer(wirespecSerialization).from(req)
    }


    @OptIn(ExperimentalEncodingApi::class)
    fun HttpServletRequest.toRawRequest(): Wirespec.RawRequest {
        if (contentType?.startsWith(MediaType.MULTIPART_FORM_DATA_VALUE) == true) {
            val req = this as MultipartHttpServletRequest
            val map: Map<String, Any> = req.multiFileMap.values.map { it.first() }.associate {
                val contentType = it.contentType ?: error("No content type found for file ${it.originalFilename}")
                val mediaType = MediaType.valueOf(contentType)
                val bytes = it.inputStream.readAllBytes()
                it.name to when (mediaType) {
                    MediaType.APPLICATION_JSON -> objectMapper.readTree(bytes)
                    MediaType.TEXT_PLAIN -> bytes
                    else -> error("Unsupported media type $mediaType")
                }
            }
            return Wirespec.RawRequest(
                method,
                extractPath(),
                extractQueries(),
                headerNames.toList().associateWith { getHeaders(it).toList() },
                objectMapper.writeValueAsBytes(map),
            )
        }
        return Wirespec.RawRequest(
            method,
            extractPath(),
            extractQueries(),
            headerNames.toList().associateWith { getHeaders(it).toList() },
            inputStream.readAllBytes(),
        )
    }
}
