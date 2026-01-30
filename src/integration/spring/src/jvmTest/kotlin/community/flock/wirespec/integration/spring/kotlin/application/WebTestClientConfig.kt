package community.flock.wirespec.integration.spring.kotlin.application

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient
import org.springframework.web.context.WebApplicationContext

@TestConfiguration
open class WebTestClientConfig {

    @Bean
    open fun webTestClient(context: WebApplicationContext): WebTestClient = MockMvcWebTestClient
        .bindToApplicationContext(context)
        .build()
}
