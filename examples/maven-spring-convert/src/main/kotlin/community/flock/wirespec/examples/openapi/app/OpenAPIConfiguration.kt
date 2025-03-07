package community.flock.wirespec.examples.openapi.app

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class OpenAPIConfiguration {

    @Bean
    fun restTemplate() = RestTemplate()
}
