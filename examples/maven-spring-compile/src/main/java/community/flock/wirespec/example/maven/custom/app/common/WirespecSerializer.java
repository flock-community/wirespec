package community.flock.wirespec.example.maven.custom.app.common;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.example.maven.custom.app.exception.SerializationException;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.java.serde.DefaultParamSerialization;
import community.flock.wirespec.java.serde.DefaultPathSerialization;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;

/**
 * Example implementation of Wirespec Serialization using DefaultParamSerialization
 * This class handles standard parameter serialization for headers and query parameters.
 * For custom serialization requirements, you can create your own implementation
 * of Wirespec.ParamSerialization instead of using DefaultParamSerialization.
 * In this case, you don't need the dependency on community.flock.wirespec.integration:wirespec
 */
@Component
public class WirespecSerializer implements Wirespec.Serialization, DefaultParamSerialization, DefaultPathSerialization {

    private final ObjectMapper objectMapper;

    public WirespecSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> byte[] serializeBody(T body, Type type) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserializeBody(byte[] raw, Type valueType) {
        if (raw == null) {
            return null;
        }
        try {
            if (valueType == String.class) {
                return (T) raw.toString();
            }
            return objectMapper.readValue(raw, objectMapper.constructType(valueType));
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }
}
