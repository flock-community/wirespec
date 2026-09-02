package community.flock.wirespec.examples.maven.spring.integration.client;

import community.flock.wirespec.java.Wirespec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Wires a {@link WirespecRestClient} using Spring Boot 4's HTTP Service Client infrastructure.
 *
 * <p>The {@code "todo"} HTTP service group is declared with {@link ImportHttpServices}. Its
 * {@code RestClient} is configured both declaratively, through {@code
 * spring.http.client.service.group.todo.*} properties (see {@code application.yaml}), and
 * programmatically, through the {@link RestClientHttpServiceGroupConfigurer} bean below. The
 * Wirespec client reuses that same group configuration to drive typed {@code Request}/{@code
 * Response} calls.
 *
 * @see <a href="https://spring.io/blog/2025/09/23/http-service-client-enhancements">HTTP Service
 *     Client Enhancements</a>
 */
@Configuration
@ImportHttpServices(group = "todo", types = TodoServiceClient.class)
public class WirespecClientConfig {

    /**
     * Programmatic configuration for the {@code "todo"} group's {@link RestClient}. Equivalent to,
     * and applied on top of, the {@code spring.http.client.service.group.todo.*} properties.
     */
    @Bean
    RestClientHttpServiceGroupConfigurer todoGroupConfigurer() {
        return groups -> groups.filterByName("todo")
                .forEachClient((group, builder) -> builder.defaultHeader("User-Agent", "wirespec-todo-client"));
    }

    /**
     * The Wirespec client, backed by a {@link RestClient} that shares the {@code "todo"} group's base
     * url. The autoconfigured {@link RestClient.Builder} already carries the global {@code
     * spring.http.client.service.*} settings (timeouts, redirects, SSL bundles, ...).
     */
    @Bean
    WirespecRestClient wirespecRestClient(
            RestClient.Builder builder,
            Wirespec.Serialization serialization,
            @Value("${spring.http.client.service.group.todo.base-url}") String baseUrl) {
        RestClient restClient =
                builder.baseUrl(baseUrl).defaultHeader("User-Agent", "wirespec-todo-client").build();
        return new WirespecRestClient(restClient, serialization);
    }
}
