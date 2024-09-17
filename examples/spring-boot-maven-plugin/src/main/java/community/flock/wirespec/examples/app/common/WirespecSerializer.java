package community.flock.wirespec.examples.app.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.java.Wirespec;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

@Component
public class WirespecSerializer implements Wirespec.Serialization<String> {

    private final ObjectMapper objectMapper;

    public WirespecSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> String serialize(T t, Type type) {
        try {
            return objectMapper.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(String s, Type type) {
        try {
            var t = objectMapper.constructType(type);
            objectMapper.readValue(s, t);
            return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
