package community.flock.wirespec.integration.spring.java.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@org.springframework.context.annotation.Configuration
public class Configuration {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public Configuration() {
        log.info("Initializing application configuration");
    }

    @Bean("wirespecSpringWebClient")
    public WebClient webClientForWirespec(@Value("${server.port}") int port) {
        log.info("Creating custom webClient");
        return WebClient.create("http://localhost:" + port);
    }
}
