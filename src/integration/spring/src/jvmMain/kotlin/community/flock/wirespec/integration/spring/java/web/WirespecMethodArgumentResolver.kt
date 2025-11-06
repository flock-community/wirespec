package community.flock.wirespec.integration.spring.java.web

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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class WirespecMethodArgumentResolver(
    private val wirespecSerializationMap: Map<MediaType, Wirespec.Serialization>,
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
        val jsonSerde = wirespecSerializationMap[MediaType.APPLICATION_JSON]
            ?: error("No serialization found for media type ${MediaType.APPLICATION_JSON_VALUE}")
        val req = servletRequest.toRawRequest()
        return instance.getServer(jsonSerde).from(req)
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun HttpServletRequest.toRawRequest(): Wirespec.RawRequest {
    if (contentType == MediaType.MULTIPART_FORM_DATA_VALUE) {
        val req = this as MultipartHttpServletRequest
        val map: Map<String, Any> = req.multiFileMap.values.map { it.first() }.associate {
            val contentType = it.contentType ?: error("No content type found for file ${it.originalFilename}")
            val mediaType = MediaType.valueOf(contentType)
            val bytes = it.inputStream.readAllBytes()
            it.name to when (mediaType) {
                MediaType.APPLICATION_JSON -> bytes.decodeToString()
                MediaType.TEXT_PLAIN -> "\"" + Base64.encode(bytes) + "\""
                else -> error("Unsupported media type $mediaType")
            }
        }
        return Wirespec.RawRequest(
            method,
            extractPath(),
            extractQueries(),
            headerNames.toList().associateWith { getHeaders(it).toList() },
            map.toList().joinToString(",", "{", "}") { """"${it.first}": ${it.second}""" }.toByteArray(),
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
