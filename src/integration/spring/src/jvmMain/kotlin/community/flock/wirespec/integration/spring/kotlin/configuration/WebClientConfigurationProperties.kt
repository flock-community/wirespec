package community.flock.wirespec.integration.spring.kotlin.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wirespec.spring.webclient")
data class WebClientConfigurationProperties(
    var baseUrl: String = "http://localhost:8080",
)
