package community.flock.wirespec.integration.spring.kotlin.application

import org.slf4j.LoggerFactory.getLogger
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
open class Configuration {
    private val log = getLogger(javaClass)

    init {
        log.info("Initializing application configuration")
    }

    @Bean("wirespecSpringWebClient")
    open fun webClientForWirespec(): WebClient {
        log.info("Creating custom webClient")
        return WebClient.create("http://localhost:8080")
    }
}
