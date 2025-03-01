package community.flock.wirespec.integration.spring.kotlin.configuration

import community.flock.wirespec.integration.spring.kotlin.client.WirespecWebClient
import community.flock.wirespec.integration.spring.shared.WebClientConfigurationProperties
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnClass(WebClient::class)
@EnableConfigurationProperties(WebClientConfigurationProperties::class)
open class WirespecWebClientConfiguration(
    val serialization: Wirespec.Serialization<String>,
) {
    @Bean("wirespecSpringWebClient")
    @ConditionalOnMissingBean(name = ["wirespecSpringWebClient"])
    open fun defaultWebClient(webClientConfigurationProperties: WebClientConfigurationProperties): WebClient {
        check(!webClientConfigurationProperties.baseUrl.isNullOrEmpty()) {
            "Could not autowire a Wirespec specific WebClient, as there was no base url configured. " +
                "Please configure one through the application property wirespec.spring.webclient.base-url." +
                "Alternatively, you could create a Webclient bean with a @Qualifier(\"wirespecSpringWebClient\") " +
                "annotation yourself. "
        }
        return WebClient.builder().baseUrl(webClientConfigurationProperties.baseUrl).build()
    }

    @Bean
    @ConditionalOnMissingBean(WirespecWebClient::class)
    open fun wirespecWebClient(
        @Qualifier("wirespecSpringWebClient") webClient: WebClient,
    ): WirespecWebClient = WirespecWebClient(
        client = webClient,
        wirespecSerde = serialization,
    )
}
