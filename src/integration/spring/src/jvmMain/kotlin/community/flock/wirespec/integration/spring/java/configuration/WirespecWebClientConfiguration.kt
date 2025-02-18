package community.flock.wirespec.integration.spring.java.configuration

import community.flock.wirespec.integration.spring.java.client.WirespecWebClient
import community.flock.wirespec.java.Wirespec
import org.springframework.beans.factory.annotation.Qualifier
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
    @Bean
    @ConditionalOnMissingBean(name = ["wirespecSpringWebClient"])
    open fun defaultWebClient(): WebClient = WebClient.builder().build()

    @Bean
    @ConditionalOnMissingBean(WirespecWebClient::class)
    open fun wirespecWebClient(@Qualifier("wirespecSpringWebClient") webClient: WebClient): WirespecWebClient = WirespecWebClient(
        client = webClient,
        wirespecSerde = serialization,
    )
}
