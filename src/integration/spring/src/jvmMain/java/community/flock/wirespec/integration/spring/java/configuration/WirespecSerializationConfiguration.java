package community.flock.wirespec.integration.spring.java.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import community.flock.wirespec.integration.jackson.java.WirespecSerialization;
import community.flock.wirespec.integration.spring.java.web.WirespecResponseBodyAdvice;
import community.flock.wirespec.java.Wirespec.Serialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({WirespecResponseBodyAdvice.class, WirespecWebMvcConfiguration.class})
public class WirespecSerializationConfiguration {

    public static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

    @Bean
    public Serialization wirespecSerialization() {
        return new WirespecSerialization(objectMapper);
    }
}
