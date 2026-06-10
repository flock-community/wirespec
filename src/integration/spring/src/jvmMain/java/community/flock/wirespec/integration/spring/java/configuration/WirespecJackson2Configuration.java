package community.flock.wirespec.integration.spring.java.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import community.flock.wirespec.integration.jackson.v2.java.WirespecSerialization;
import community.flock.wirespec.integration.spring.shared.Jackson2JsonMapper;
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper;
import community.flock.wirespec.java.Wirespec.Serialization;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Active when Jackson 3 is absent (Spring Boot 3, Jackson 2 only). Backs off whenever
 * Jackson 3 is present so {@link WirespecJackson3Configuration} wins.
 */
@Configuration
@ConditionalOnMissingClass("tools.jackson.databind.json.JsonMapper")
public class WirespecJackson2Configuration {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    @Bean
    public Serialization wirespecSerialization() {
        return new WirespecSerialization(objectMapper);
    }

    @Bean
    public WirespecJsonMapper wirespecJsonMapper() {
        return new Jackson2JsonMapper(objectMapper);
    }
}
