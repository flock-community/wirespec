package community.flock.wirespec.integration.wiremock.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.UrlPattern
import community.flock.wirespec.integration.jackson.kotlin.WirespecSerialization
import community.flock.wirespec.kotlin.Wirespec

/**
 * Start building a WireMock stub for a Wirespec endpoint. Mirrors WireMock's own
 * `get(urlEqualTo(...))` / `post(urlEqualTo(...))` factories — the returned builder
 * then accepts a typed Wirespec [Wirespec.Response] via [WirespecMappingBuilder.willReturn].
 *
 * Two calling styles:
 *
 * ```
 * // Pass the endpoint's Server companion directly:
 * server.stubFor(wirespec(GetTodos.Handler).willReturn(GetTodos.Response200(todos)))
 *
 * // Or use the reified-type overload to skip the .Handler:
 * server.stubFor(wirespec<GetTodos>().willReturn(GetTodos.Response200(todos)))
 * ```
 *
 * The endpoint's HTTP method and path template drive the WireMock request matcher
 * (path parameters match any non-slash segment).
 */
fun wirespec(endpoint: Wirespec.Server<*, *>): WirespecMappingBuilder = WirespecMappingBuilder(endpoint, requestBuilder(endpoint))

/**
 * Reified-type overload — `wirespec<GetTodos>()` reflectively locates the
 * [Wirespec.Server] implementation generated inside the endpoint type.
 */
inline fun <reified E : Wirespec.Endpoint> wirespec(): WirespecMappingBuilder = wirespec(findServer(E::class.java))

class WirespecMappingBuilder internal constructor(
    private val endpoint: Wirespec.Server<*, *>,
    private val mapping: MappingBuilder,
) {
    /**
     * Serialize [response] through [serialization] and attach it as this stub's response.
     * Defaults to a Jackson-backed [Wirespec.Serialization]; pass your own to customize
     * the ObjectMapper or swap in a different serializer. Returns the underlying
     * [MappingBuilder] so callers can keep chaining WireMock methods (e.g. `.atPriority(...)`,
     * `.inScenario(...)`).
     */
    @Suppress("UNCHECKED_CAST")
    fun willReturn(
        response: Wirespec.Response<*>,
        serialization: Wirespec.Serialization = defaultSerialization,
    ): MappingBuilder {
        val typed = endpoint as Wirespec.Server<*, Wirespec.Response<*>>
        return mapping.willReturn(responseBuilder(typed.server(serialization).to(response)))
    }
}

@PublishedApi
internal fun findServer(endpointClass: Class<*>): Wirespec.Server<*, *> = findServerRecursive(endpointClass)
    ?: throw IllegalArgumentException(
        "No Wirespec.Server implementation with a no-arg constructor found inside ${endpointClass.name}. " +
            "Expected the generated <Endpoint>.Handler structure.",
    )

private fun findServerRecursive(klass: Class<*>): Wirespec.Server<*, *>? {
    if (Wirespec.Server::class.java.isAssignableFrom(klass) && !klass.isInterface) {
        // Kotlin companion objects expose a static INSTANCE field; Java-emitted Handlers
        // are records with a public no-arg constructor.
        runCatching {
            val instanceField = klass.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return instanceField.get(null) as Wirespec.Server<*, *>
        }
        runCatching {
            val constructor = klass.getDeclaredConstructor()
            constructor.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return constructor.newInstance() as Wirespec.Server<*, *>
        }
    }
    klass.declaredClasses.forEach { nested ->
        findServerRecursive(nested)?.let { return it }
    }
    return null
}

private val defaultSerialization: Wirespec.Serialization by lazy { WirespecSerialization(ObjectMapper()) }

private fun requestBuilder(endpoint: Wirespec.Server<*, *>): MappingBuilder {
    val urlPattern = urlPatternFor(endpoint.pathTemplate)
    return when (endpoint.method.uppercase()) {
        "GET" -> WireMock.get(urlPattern)
        "PUT" -> WireMock.put(urlPattern)
        "POST" -> WireMock.post(urlPattern)
        "DELETE" -> WireMock.delete(urlPattern)
        "PATCH" -> WireMock.patch(urlPattern)
        "HEAD" -> WireMock.head(urlPattern)
        "OPTIONS" -> WireMock.options(urlPattern)
        "TRACE" -> WireMock.trace(urlPattern)
        else -> WireMock.any(urlPattern)
    }
}

private fun responseBuilder(rawResponse: Wirespec.RawResponse): ResponseDefinitionBuilder {
    val builder = WireMock.aResponse().withStatus(rawResponse.statusCode)
    rawResponse.headers.forEach { (name, values) ->
        values.forEach { value -> builder.withHeader(name, value) }
    }
    rawResponse.body?.let(builder::withBody)
    return builder
}

private val PATH_PARAM_REGEX = Regex("""\{[^/}]+\}""")

internal fun urlPatternFor(pathTemplate: String): UrlPattern = if (PATH_PARAM_REGEX.containsMatchIn(pathTemplate)) {
    val regex = pathTemplate.split(PATH_PARAM_REGEX).joinToString("[^/]+") { Regex.escape(it) }
    WireMock.urlPathMatching(regex)
} else {
    WireMock.urlPathEqualTo(pathTemplate)
}
