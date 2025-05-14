package community.flock.wirespec.java.serde;

import community.flock.wirespec.java.Wirespec;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultParamSerializationTest {

    private final DefaultParamSerialization serde = DefaultParamSerialization.create();
    @Test
    public void shouldSerializePrimitiveTypesCorrectly() {
        primitiveTestCases().forEach(testCase ->
                assertEquals(testCase.expected, serde.serializeParam(testCase.value, testCase.type))
        );
    }

    record PrimitiveTestCase(
            String name,
            Object value,
            Type type,
            List<String> expected
    ){}

    public static List<PrimitiveTestCase> primitiveTestCases() {
        return Arrays.asList(
                new PrimitiveTestCase(
                        "string",
                        "test",
                        String.class,
                        List.of("test")
                ),
                new PrimitiveTestCase(
                        "int",
                        42,
                        Integer.class,
                        List.of("42")
                ),
                new PrimitiveTestCase(
                        "long",
                        42L,
                        Long.class,
                        List.of("42")
                ),
                new PrimitiveTestCase(
                        "double",
                        42.0,
                        Double.class,
                        List.of("42.0")
                ),
                new PrimitiveTestCase(
                        "float",
                        42.0f,
                        Float.class,
                        List.of("42.0")
                ),
                new PrimitiveTestCase(
                        "boolean",
                        true,
                        Boolean.class,
                        List.of("true")
                ),
                new PrimitiveTestCase(
                        "char",
                        'a',
                        Character.class,
                        List.of("a")
                ),
                new PrimitiveTestCase(
                        "byte",
                        (byte) 42,
                        Byte.class,
                        List.of("42")
                ),
                new PrimitiveTestCase(
                        "short",
                        (short) 42,
                        Short.class,
                        List.of("42")
                )
        );
    }




}

