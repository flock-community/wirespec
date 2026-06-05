package community.flock.wirespec.integration.spring.java.configuration;

import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Entry point imported by {@code @EnableWirespecController}. The Wirespec Serialization
 * bean and the multipart WirespecJsonMapper bean are contributed by one of the two
 * version-conditional configurations, so the integration runs on both Spring Boot 3
 * (Jackson 2) and Spring Boot 4 (Jackson 3). Jackson 3 takes precedence when both are present.
 */
@Configuration
@Import({
        WirespecResponseBodyAdvice.class,
        WirespecWebMvcConfiguration.class,
        WirespecNativeConfiguration.class,
        WirespecJackson3Configuration.class,
        WirespecJackson2Configuration.class,
})
public class WirespecSerializationConfiguration {
}
