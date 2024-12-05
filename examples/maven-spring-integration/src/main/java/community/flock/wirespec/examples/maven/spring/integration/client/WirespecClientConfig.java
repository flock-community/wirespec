package community.flock.wirespec.examples.maven.spring.integration.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WirespecClientConfig {

  @Bean("wirespecSpringWebClient")
  public WebClient webClient(
      WebClient.Builder builder
  ) {
    return builder.baseUrl("http://localhost:8080").build();
  }
}
