package community.flock.wirespec.integration.spring.kotlin.configuration

import community.flock.wirespec.integration.spring.kotlin.client.WirespecHttpExchange
import community.flock.wirespec.integration.spring.kotlin.client.WirespecTransportation
import community.flock.wirespec.integration.spring.shared.WebClientConfigurationProperties
import community.flock.wirespec.kotlin.Wirespec
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

/**
 * Wires a Spring-backed [Wirespec.Transportation] for the generated `<Endpoint>.Call` clients.
 *
 * The transport delegates to a [WirespecHttpExchange] proxy built with [HttpServiceProxyFactory] on top of a
 * reactive [WebClient]. Consumers combine the resulting [Wirespec.Transportation] bean with the
 * [Wirespec.Serialization] bean to construct a generated `Client`/`<Endpoint>Client`.
 *
 * The base url is read from the shared `wirespec.spring.webclient.base-url` property; supply a
 * [WirespecHttpExchange] or [Wirespec.Transportation] bean of your own to override the defaults.
 */
@Configuration
@ConditionalOnClass(HttpServiceProxyFactory::class, WebClient::class)
@EnableConfigurationProperties(WebClientConfigurationProperties::class)
@Order(Ordered.LOWEST_PRECEDENCE)
open class WirespecTransportationConfiguration {
    private val log: Logger = getLogger(javaClass)

    @Bean("wirespecHttpExchange")
    @ConditionalOnMissingBean(WirespecHttpExchange::class)
    open fun wirespecHttpExchange(): WirespecHttpExchange {
        log.debug("Initializing a Wirespec HttpServiceProxy through HttpServiceProxyFactory")
        val adapter = WebClientAdapter.create(WebClient.create())
        return HttpServiceProxyFactory.builderFor(adapter).build()
            .createClient(WirespecHttpExchange::class.java)
    }

    @Bean
    @ConditionalOnMissingBean(Wirespec.Transportation::class)
    open fun wirespecTransportation(
        wirespecHttpExchange: WirespecHttpExchange,
        webClientConfigurationProperties: WebClientConfigurationProperties,
    ): Wirespec.Transportation {
        check(!webClientConfigurationProperties.baseUrl.isNullOrEmpty()) {
            "Could not autowire a Wirespec Transportation, as there was no base url configured. " +
                "Please configure one through the application property wirespec.spring.webclient.base-url."
        }
        log.debug("Initializing a Wirespec Transportation backed by an HttpServiceProxy")
        return WirespecTransportation(wirespecHttpExchange, webClientConfigurationProperties.baseUrl)
    }
}
