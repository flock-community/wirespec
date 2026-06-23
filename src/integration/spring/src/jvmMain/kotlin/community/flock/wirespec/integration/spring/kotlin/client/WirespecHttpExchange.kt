package community.flock.wirespec.integration.spring.kotlin.client

import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.HttpExchange
import reactor.core.publisher.Mono
import java.net.URI

/**
 * Declarative HTTP interface proxied by Spring's [org.springframework.web.service.invoker.HttpServiceProxyFactory].
 *
 * Wirespec already lowers every endpoint to a [community.flock.wirespec.kotlin.Wirespec.RawRequest] /
 * [community.flock.wirespec.kotlin.Wirespec.RawResponse] pair, so a single generic exchange method is
 * enough: the HTTP method, the (base-url relative) target URI, the headers and the raw body are all passed
 * as arguments and resolved by the factory's default argument resolvers. [WirespecTransportation] adapts
 * this proxy to the [community.flock.wirespec.kotlin.Wirespec.Transportation] contract consumed by the
 * generated `<Endpoint>.Call` clients.
 */
interface WirespecHttpExchange {
    @HttpExchange
    fun exchange(
        method: HttpMethod,
        uri: URI,
        @RequestHeader headers: MultiValueMap<String, String>,
        @RequestBody body: ByteArray?,
    ): Mono<ResponseEntity<ByteArray>>
}
