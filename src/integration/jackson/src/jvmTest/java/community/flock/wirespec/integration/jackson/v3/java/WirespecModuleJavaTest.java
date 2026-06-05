package community.flock.wirespec.integration.jackson.v3.java;

import community.flock.wirespec.integration.jackson.java.generated.model.*;
import org.junit.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.Assert.assertEquals;

public class WirespecModuleJavaTest {

    Todo todo = new Todo(
            new TodoId("123"),
            "Do It now",
            false,
            TodoCategory.WORK,
            "test@wirespec.nl"
    );

    String todoJson =
            //language=json
            """
                    {
                      "id" : "123",
                      "name" : "Do It now",
                      "final" : false,
                      "category" : "WORK",
                      "eMail" : "test@wirespec.nl"
                    }""";

    TypeWithAllRefined typeWithAllRefined = new TypeWithAllRefined(
            new StringRefinedRegex("string refined regex"),
            new StringRefined("string refined"),
            new IntRefinedNoBound(1L),
            new IntRefinedLowerBound(2L),
            new IntRefinedUpperBound(3L),
            new IntRefinedLowerAndUpper(4L),
            new NumberRefinedNoBound(1.0),
            new NumberRefinedLowerBound(2.0),
            new NumberRefinedUpperBound(3.0),
            new NumberRefinedLowerAndUpper(4.0)
    );

    String typeJson =
            //language=json
            """
                    {
                      "stringRefinedRegex" : "string refined regex",
                      "stringRefined" : "string refined",
                      "intRefinedNoBound" : 1,
                      "intRefinedLowerBound" : 2,
                      "intRefinedUpperBound" : 3,
                      "intRefinedLowerAndUpper" : 4,
                      "numberRefinedNoBound" : 1.0,
                      "numberRefinedLowerBound" : 2.0,
                      "numberRefinedUpperBound" : 3.0,
                      "numberRefinedLowerAndUpper" : 4.0
                    }""";


    ObjectMapper objectMapper = JsonMapper.builder()
            .propertyNamingStrategy(new WirespecModuleJava.JavaReservedKeywordNamingStrategy())
            .addModule(new WirespecModuleJava())
            .build();

    @Test
    public void serializeJavaRefined() {
        var res = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(todo);
        assertEquals(todoJson, res);
    }

    @Test
    public void deserializeJavaRefined() {
        var res = objectMapper.readValue(todoJson, Todo.class);
        assertEquals(todo, res);
    }

    @Test
    public void serializeJavaRefined2() {
        var res = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(typeWithAllRefined);
        assertEquals(typeJson, res);
    }

    @Test
    public void deserializeJavaRefined2() {
        var res = objectMapper.readValue(typeJson, TypeWithAllRefined.class);
        assertEquals(typeWithAllRefined, res);
    }
}
