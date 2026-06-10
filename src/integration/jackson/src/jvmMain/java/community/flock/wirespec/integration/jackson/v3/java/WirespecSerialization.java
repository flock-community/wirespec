package community.flock.wirespec.integration.jackson.v3.java;

import community.flock.wirespec.java.Wirespec.Serialization;
import community.flock.wirespec.java.serde.DefaultParamSerialization;
import community.flock.wirespec.java.serde.DefaultPathSerialization;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * A reusable implementation of Wirespec.Serialization backed by Jackson 3.
 *
 * It rebuilds the supplied (immutable) {@link JsonMapper} with the
 * {@link WirespecModuleJava} registered. Java records expose their components through
 * accessors, so no visibility configuration is required. Parameter and path
 * serialization are delegated to the default Wirespec serializers.
 */
public class WirespecSerialization implements Serialization, DefaultParamSerialization, DefaultPathSerialization {

    private final JsonMapper wirespecObjectMapper;

    public WirespecSerialization(JsonMapper jsonMapper) {
        // The reserved-keyword naming strategy must be set on the builder
        // (Jackson 3 modules cannot register one).
        this.wirespecObjectMapper = jsonMapper.rebuild()
                .propertyNamingStrategy(new WirespecModuleJava.JavaReservedKeywordNamingStrategy())
                .addModule(new WirespecModuleJava())
                .build();
    }

    @Override
    public <T> byte[] serializeBody(T body, Type type) {
        if (body instanceof String) {
            return ((String) body).getBytes(StandardCharsets.UTF_8);
        } else {
            return wirespecObjectMapper.writeValueAsBytes(body);
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
            JavaType type = wirespecObjectMapper.constructType(valueType);
            return wirespecObjectMapper.readValue(raw, type);
        }
    }
}
