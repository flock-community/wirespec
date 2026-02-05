package community.flock.wirespec.integration.spring.java.application;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

@TestConfiguration
public class WebTestClientConfig {

    @Bean
    public WebTestClient webTestClient(WebApplicationContext context) {
        return MockMvcWebTestClient
                .bindToApplicationContext(context)
                .build();
    }
}
