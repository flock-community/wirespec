package community.flock.wirespec.example.maven.preprocessor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ExamplePreProcessor implements Function<String, String> {

    @Override
    public String apply(String s) {
        var objectMapper = new ObjectMapper();
        try {
            var openapi = objectMapper.readTree(s);
            var paths = openapi.get("paths");
            var spliterator  = Spliterators.spliteratorUnknownSize(paths.fields(), Spliterator.ORDERED);
            var filteredPaths = StreamSupport.stream(spliterator, false)
                    .filter(it -> it.getKey().equals("/pet"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            ((ObjectNode) openapi).set("paths", objectMapper.valueToTree(filteredPaths));
            return objectMapper.writeValueAsString(openapi);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot process openapi json file", e);
        }
    }

}
