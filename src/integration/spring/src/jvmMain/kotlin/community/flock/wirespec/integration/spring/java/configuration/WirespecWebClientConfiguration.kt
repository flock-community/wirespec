package community.flock.wirespec.integration.spring.java.configuration

import community.flock.wirespec.integration.spring.java.client.WirespecWebClient
import community.flock.wirespec.integration.spring.shared.WebClientConfigurationProperties
import community.flock.wirespec.java.Wirespec.Serialization
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnClass(WebClient::class)
@ConditionalOnMissingBean(WirespecWebClient::class)
@EnableConfigurationProperties(WebClientConfigurationProperties::class)
@Order(Ordered.LOWEST_PRECEDENCE)
open class WirespecWebClientConfiguration(
    val serializationMap: Map<MediaType, Serialization>,
) {
    private val log: Logger = getLogger(javaClass)

    @Bean("wirespecSpringWebClient")
    @ConditionalOnMissingBean(name = ["wirespecSpringWebClient"])
    open fun defaultWebClient(webClientConfigurationProperties: WebClientConfigurationProperties): WebClient {
        log.debug("Initializing a Spring WebClient for Wirespec with qualifier \"wirespecSpringWebClient\"")
        check(!webClientConfigurationProperties.baseUrl.isNullOrEmpty()) {
            "Could not autowire a Wirespec specific WebClient, as there was no base url configured. " +
                "Please configure one through the application property wirespec.spring.webclient.base-url." +
                "Alternatively, you could create a Webclient bean with a @Qualifier(\"wirespecSpringWebClient\") " +
                "annotation yourself. "
        }
        return WebClient.create(webClientConfigurationProperties.baseUrl)
    }

    @Bean
    @ConditionalOnMissingBean(WirespecWebClient::class)
    open fun wirespecWebClient(
        @Qualifier("wirespecSpringWebClient") webClient: WebClient,
    ): WirespecWebClient {
        log.debug("Initializing WirespecWebclient for Wirespec, wrapping a Spring WebClient")
        return WirespecWebClient(
            client = webClient,
            wirespecSerdeMap = serializationMap,
        )
    }
}
