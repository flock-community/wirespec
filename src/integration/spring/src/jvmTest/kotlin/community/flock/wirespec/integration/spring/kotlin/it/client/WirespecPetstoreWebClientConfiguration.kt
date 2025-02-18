package community.flock.wirespec.integration.spring.kotlin.it.client

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
open class WirespecPetstoreWebClientConfiguration {

    @Bean("wirespecSpringWebClient")
    open fun webClient(): WebClient = WebClient.builder().baseUrl("http://localhost:8080").build()
}
