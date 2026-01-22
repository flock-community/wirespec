package community.flock.wirespec.integration.jackson.java;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import community.flock.wirespec.java.Wirespec.Serialization;
import community.flock.wirespec.java.serde.DefaultParamSerialization;
import community.flock.wirespec.java.serde.DefaultPathSerialization;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * A reusable implementation of Wirespec.Serialization that uses Jackson for serialization and deserialization.
 * This class implements parameter serialization and deserialization using a private ParamSerialization field.
 */
public class WirespecSerialization implements Serialization, DefaultParamSerialization, DefaultPathSerialization {

    private final ObjectMapper wirespecObjectMapper;

    public WirespecSerialization(ObjectMapper objectMapper) {
        this.wirespecObjectMapper = objectMapper.copy()
                .registerModule(new WirespecModuleJava());
    }

    @Override
    public <T> byte[] serializeBody(T body, Type type) {
        if (body instanceof String) {
            return ((String) body).getBytes(StandardCharsets.UTF_8);
        } else {
            try {
                return wirespecObjectMapper.writeValueAsBytes(body);
            } catch (IOException e) {
                throw new RuntimeException("Failed to serialize body", e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserializeBody(byte[] raw, Type valueType) {
        if (raw == null) {
            return null;
        }

        if (valueType.equals(String.class)) {
                return (T) new String(raw, StandardCharsets.UTF_8);
            } else {
            try {
                JavaType type = wirespecObjectMapper.getTypeFactory().constructType(valueType);
                return wirespecObjectMapper.readValue(raw, type);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize body", e);
            }
        }
    }
}