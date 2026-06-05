package community.flock.wirespec.integration.spring.java.configuration;

import community.flock.wirespec.integration.jackson.v3.java.WirespecSerialization;
import community.flock.wirespec.integration.spring.shared.Jackson3JsonMapper;
import community.flock.wirespec.integration.spring.shared.WirespecJsonMapper;
import community.flock.wirespec.java.Wirespec.Serialization;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Active when Jackson 3 is on the classpath (Spring Boot 4). Takes precedence over
 * {@link WirespecJackson2Configuration}, which backs off whenever Jackson 3 is present.
 */
@Configuration
// Referenced by name (not JsonMapper.class): the selection tests register this config by
// class on a Jackson-3-free classpath, where resolving a Class-valued condition would fail.
@ConditionalOnClass(name = "tools.jackson.databind.json.JsonMapper")
public class WirespecJackson3Configuration {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Bean
    public Serialization wirespecSerialization() {
        return new WirespecSerialization(jsonMapper);
    }

    @Bean
    public WirespecJsonMapper wirespecJsonMapper() {
        return new Jackson3JsonMapper(jsonMapper);
    }
}
