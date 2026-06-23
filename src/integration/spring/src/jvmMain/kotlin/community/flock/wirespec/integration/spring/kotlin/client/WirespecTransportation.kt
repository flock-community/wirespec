package community.flock.wirespec.integration.spring.kotlin.client

import community.flock.wirespec.integration.spring.shared.filterNotEmpty
import community.flock.wirespec.kotlin.Wirespec
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

/**
 * Spring-backed [Wirespec.Transportation] for the generated `<Endpoint>.Call` clients.
 *
 * Where [WirespecWebClient] drives the `<Endpoint>.Handler` client edge directly, this implements the
 * lower-level [Wirespec.Transportation] contract that a generated `Client`/`<Endpoint>Client` delegates to,
 * so the high-level, typed `<Endpoint>.Call` interface can be used end to end:
 *
 * ```kotlin
 * val client = Client(serialization, wirespecTransportation)
 * client.getTodos(done = true)
 * ```
 *
 * The actual HTTP exchange is delegated to a [WirespecHttpExchange] proxy created by
 * [org.springframework.web.service.invoker.HttpServiceProxyFactory]. Every [Wirespec.RawRequest] carries its
 * path as a list of segments, so the target is resolved against [baseUrl] here rather than relying on the
 * proxy's base-url handling (a relative `URI` argument is passed through unresolved by Spring's HTTP service).
 */
class WirespecTransportation(
    private val exchange: WirespecHttpExchange,
    private val baseUrl: String,
) : Wirespec.Transportation {

    override suspend fun transport(request: Wirespec.RawRequest): Wirespec.RawResponse = try {
        exchange.exchange(
            method = HttpMethod.valueOf(request.method),
            uri = request.toUri(),
            headers = request.toHttpHeaders(),
            body = request.body,
        ).awaitSingle().let { response ->
            Wirespec.RawResponse(
                statusCode = response.statusCode.value(),
                headers = response.headers.toRawHeaders(),
                body = response.body,
            )
        }
    } catch (e: WebClientResponseException) {
        // A non-2xx status is a regular Wirespec response, not a failure: surface it as a RawResponse
        // so `fromRawResponse` can map it onto the matching Response<*> variant.
        Wirespec.RawResponse(
            statusCode = e.statusCode.value(),
            headers = e.headers.toRawHeaders(),
            body = e.responseBodyAsByteArray,
        )
    }

    private fun Wirespec.RawRequest.toUri(): URI = UriComponentsBuilder.fromUriString(baseUrl)
        .pathSegment(*path.toTypedArray())
        .apply {
            queries.filterNotEmpty().forEach { (key, values) ->
                values.forEach { queryParam(key, it) }
            }
        }
        .build()
        .toUri()

    private fun Wirespec.RawRequest.toHttpHeaders(): MultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
        headers.filterNotEmpty().forEach { (key, values) -> addAll(key, values) }
        if (body != null && !containsKey(HttpHeaders.CONTENT_TYPE)) {
            add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        }
    }

    /**
     * Converts Spring [HttpHeaders] into the [Map] expected by [Wirespec.RawResponse].
     *
     * This deliberately avoids casting [HttpHeaders] to [Map]: in Spring Framework 6 [HttpHeaders]
     * implements [MultiValueMap] (and therefore [Map]), but in Spring Framework 7 it no longer does.
     * Iterating via [HttpHeaders.forEach] works on both versions, so a jar compiled against Spring 6
     * keeps working at runtime on Spring 7.
     */
    private fun HttpHeaders.toRawHeaders(): Map<String, List<String>> = LinkedMultiValueMap<String, String>().also { map ->
        forEach { name, values -> map[name] = values }
    }
}
