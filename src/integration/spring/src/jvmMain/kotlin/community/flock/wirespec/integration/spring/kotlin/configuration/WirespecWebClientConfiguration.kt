package community.flock.wirespec.integration.spring.kotlin.configuration

import community.flock.wirespec.integration.spring.kotlin.client.WirespecWebClient
import community.flock.wirespec.kotlin.Wirespec
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnClass(WebClient::class)
open class WirespecWebClientConfiguration(
    val serialization: Wirespec.Serialization<String>,
) {
    @Value("\${wirespec.spring.webclient.baseUrl:http://localhost:8080}")
    lateinit var webClientBaseUrl: String

    @Bean("wirespecSpringWebClient")
    @ConditionalOnMissingBean(name = ["wirespecSpringWebClient"])
    open fun defaultWebClient(): WebClient = WebClient.builder().baseUrl(webClientBaseUrl).build()

    @Bean
    @ConditionalOnMissingBean(WirespecWebClient::class)
    open fun wirespecWebClient(
        @Qualifier("wirespecSpringWebClient") webClient: WebClient,
    ): WirespecWebClient = WirespecWebClient(
        client = webClient,
        wirespecSerde = serialization,
    )
}
