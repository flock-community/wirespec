package community.flock.wirespec.integration.jackson.java;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import community.flock.wirespec.integration.jackson.java.generated.model.Todo;
import community.flock.wirespec.integration.jackson.java.generated.model.TodoCategory;
import community.flock.wirespec.integration.jackson.java.generated.model.TodoId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WirespecModuleJavaTest {

    Todo todo = new Todo(
            new TodoId("123"),
            "Do It now",
            false,
            TodoCategory.WORK,
            "test@wirespec.nl"
    );

    String json = "{\"id\":\"123\",\"name\":\"Do It now\",\"final\":false,\"category\":\"WORK\",\"eMail\":\"test@wirespec.nl\"}";

    ObjectMapper objectMapper = new ObjectMapper()
            .registerModules(new WirespecModuleJava());

    @Test
    public void serializeJavaRefined() throws JsonProcessingException {
        var res = objectMapper.writeValueAsString(todo);
        assertEquals(json, res);
    }

    @Test
    public void deserializeJavaRefined() throws JsonProcessingException {
        var res = objectMapper.readValue(json, Todo.class);
        assertEquals(todo, res);
    }
}
