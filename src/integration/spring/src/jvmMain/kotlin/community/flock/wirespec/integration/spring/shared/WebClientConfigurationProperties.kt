package community.flock.wirespec.integration.spring.shared

import org.springframework.boot.context.properties.ConfigurationProperties

// TODO Configuration metadata to use in application.yml or application.properties is NOT generated automatically
// because symbol processor KSP is not available yet
// Keep the documentation of this file in sync with [spring-configuration-metadata.json](src/integration/spring/src/jvmMain/resources/META-INF/spring-configuration-metadata.json)
// .
// See also:
// https://docs.spring.io/spring-boot/docs/3.2.0/reference/html/configuration-metadata.html
// https://github.com/spring-projects/spring-boot/issues/28046
// .
/**
 * Configuration properties for setting up a WebClient in a Spring application.
 *
 * This class is used to configure the base URL for the WebClient.
 * It is bound to properties prefixed with "wirespec.spring.webclient" in the application's configuration file.
 *
 * @property baseUrl The base URL to be used by the WebClient for making requests.
 */
@ConfigurationProperties(prefix = "wirespec.spring.webclient")
data class WebClientConfigurationProperties(
    val baseUrl: String? = null,
)
