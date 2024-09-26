package community.flock.wirespec.examples.open_api_app

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class OpenApiConfiguration {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}