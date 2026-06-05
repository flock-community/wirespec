package community.flock.wirespec.integration.jackson;

import community.flock.wirespec.integration.jackson.java.generated.model.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Shared fixtures, expected JSON and assertions for the Wirespec Jackson Java module,
 * exercised against both the Jackson 2 and Jackson 3 variants. Subclasses only provide
 * the version-specific {@link #serialize}/{@link #deserialize} backed by their mapper.
 */
public abstract class AbstractWirespecModuleJavaTest {

    protected final Todo todo = new Todo(
            new TodoId("123"),
            "Do It now",
            false,
            TodoCategory.WORK,
            "test@wirespec.nl"
    );

    protected final String todoJson =
            //language=json
            """
                    {
                      "id" : "123",
                      "name" : "Do It now",
                      "final" : false,
                      "category" : "WORK",
                      "eMail" : "test@wirespec.nl"
                    }""";

    protected final TypeWithAllRefined typeWithAllRefined = new TypeWithAllRefined(
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

    protected final String typeJson =
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

    /** Pretty-print {@code value} as JSON using the module under test. */
    protected abstract String serialize(Object value) throws Exception;

    /** Read {@code json} into {@code type} using the module under test. */
    protected abstract <T> T deserialize(String json, Class<T> type) throws Exception;

    @Test
    public void serializeJavaRefined() throws Exception {
        assertEquals(todoJson, serialize(todo));
    }

    @Test
    public void deserializeJavaRefined() throws Exception {
        assertEquals(todo, deserialize(todoJson, Todo.class));
    }

    @Test
    public void serializeJavaRefined2() throws Exception {
        assertEquals(typeJson, serialize(typeWithAllRefined));
    }

    @Test
    public void deserializeJavaRefined2() throws Exception {
        assertEquals(typeWithAllRefined, deserialize(typeJson, TypeWithAllRefined.class));
    }
}
