package community.flock.wirespec.integration.spring.kotlin.web

import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecSerializationConfiguration.Companion.objectMapper
import community.flock.wirespec.integration.spring.shared.extractPath
import community.flock.wirespec.integration.spring.shared.extractQueries
import community.flock.wirespec.kotlin.Wirespec
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.multipart.MultipartHttpServletRequest
import java.util.stream.Collectors
import kotlin.io.encoding.ExperimentalEncodingApi

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
        val endpointClass = parameter.parameterType.declaringClass
        val adapterClass = endpointClass.declaredClasses.toList().find { it.simpleName == "Adapter" } ?: error("Handler not found")
        val adapter: Wirespec.Adapter<Wirespec.Request<*>, Wirespec.Response<*>> = adapterClass.kotlin.objectInstance as? Wirespec.Adapter<Wirespec.Request<*>, Wirespec.Response<*>> ?: error("Handler not initialized")
        val rawRequest = servletRequest.toRawRequest()
        return adapter.fromRawRequest(wirespecSerialization, rawRequest)
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
                    else -> bytes
                }
            }
            return Wirespec.RawRequest(
                method = method,
                path = extractPath(),
                queries = extractQueries(),
                headers = headerNames.toList().associateWith { getHeaders(it).toList() },
                body = objectMapper.writeValueAsBytes(map),
            )
        }
        return Wirespec.RawRequest(
            method = method,
            path = extractPath(),
            queries = extractQueries(),
            headers = headerNames.toList().associateWith { getHeaders(it).toList() },
            body = reader.lines().collect(Collectors.joining(System.lineSeparator())).toByteArray(),
        )
    }
}
