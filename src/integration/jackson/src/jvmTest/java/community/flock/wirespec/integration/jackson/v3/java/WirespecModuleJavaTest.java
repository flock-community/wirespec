package community.flock.wirespec.integration.jackson.v3.java;

import community.flock.wirespec.integration.jackson.AbstractWirespecModuleJavaTest;
import tools.jackson.databind.json.JsonMapper;

public class WirespecModuleJavaTest extends AbstractWirespecModuleJavaTest {

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .propertyNamingStrategy(new WirespecModuleJava.JavaReservedKeywordNamingStrategy())
            .addModule(new WirespecModuleJava())
            .build();

    @Override
    protected String serialize(Object value) {
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    @Override
    protected <T> T deserialize(String json, Class<T> type) {
        return jsonMapper.readValue(json, type);
    }
}
