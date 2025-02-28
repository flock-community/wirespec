package community.flock.wirespec.integration.spring.java.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "wirespec.spring.webclient")
data class WebClientConfigurationProperties
    @ConstructorBinding
    constructor(
        var baseUrl: String = "http://localhost:8080",
    )
