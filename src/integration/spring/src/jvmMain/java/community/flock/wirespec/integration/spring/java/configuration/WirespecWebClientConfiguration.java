package community.flock.wirespec.integration.spring.java.configuration;

import community.flock.wirespec.integration.spring.java.client.WirespecWebClient;
import community.flock.wirespec.integration.spring.shared.WebClientConfigurationProperties;
import community.flock.wirespec.java.Wirespec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConditionalOnClass(WebClient.class)
@ConditionalOnMissingBean(WirespecWebClient.class)
@EnableConfigurationProperties(WebClientConfigurationProperties.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class WirespecWebClientConfiguration {
    private final Wirespec.Serialization serialization;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public WirespecWebClientConfiguration(Wirespec.Serialization serialization) {
        this.serialization = serialization;
    }

    @Bean("wirespecSpringWebClient")
    @ConditionalOnMissingBean(name = "wirespecSpringWebClient")
    public WebClient defaultWebClient(WebClientConfigurationProperties webClientConfigurationProperties) {
        log.debug("Initializing a Spring WebClient for Wirespec with qualifier \"wirespecSpringWebClient\"");
        if (webClientConfigurationProperties.getBaseUrl() == null || webClientConfigurationProperties.getBaseUrl().isEmpty()) {
            throw new IllegalStateException("Could not autowire a Wirespec specific WebClient, as there was no base url configured. " +
                "Please configure one through the application property wirespec.spring.webclient.base-url." +
                "Alternatively, you could create a Webclient bean with a @Qualifier(\"wirespecSpringWebClient\") " +
                "annotation yourself. ");
        }
        return WebClient.create(webClientConfigurationProperties.getBaseUrl());
    }

    @Bean
    @ConditionalOnMissingBean(WirespecWebClient.class)
    public WirespecWebClient wirespecWebClient(
        @Qualifier("wirespecSpringWebClient") WebClient webClient
    ) {
        log.debug("Initializing WirespecWebclient for Wirespec, wrapping a Spring WebClient");
        return new WirespecWebClient(webClient, serialization);
    }
}
