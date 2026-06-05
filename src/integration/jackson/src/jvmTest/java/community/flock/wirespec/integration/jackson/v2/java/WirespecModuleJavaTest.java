package community.flock.wirespec.integration.jackson.v2.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.integration.jackson.AbstractWirespecModuleJavaTest;

public class WirespecModuleJavaTest extends AbstractWirespecModuleJavaTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModules(new WirespecModuleJava());

    @Override
    protected String serialize(Object value) throws Exception {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    @Override
    protected <T> T deserialize(String json, Class<T> type) throws Exception {
        return objectMapper.readValue(json, type);
    }
}
