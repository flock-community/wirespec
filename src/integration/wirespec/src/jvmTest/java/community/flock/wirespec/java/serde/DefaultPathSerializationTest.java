package community.flock.wirespec.java.serde;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultPathSerializationTest {

    private final DefaultPathSerialization serde = new DefaultPathSerialization() {};

    @Test
    public void shouldSerializePrimitiveTypesCorrectly() {
        primitiveTestCases().forEach(testCase ->
                assertEquals(testCase.expected, serde.serializePath(testCase.value, testCase.type))
        );
    }

    @Test
    public void shouldDeserializePrimitiveTypesCorrectly() {
        primitiveTestCases().forEach(testCase ->
                assertEquals(testCase.value, serde.deserializePath(testCase.expected, testCase.type))
        );
    }

    record PrimitiveTestCase(
            String name,
            Object value,
            Type type,
            String expected
    ){}

    public static List<PrimitiveTestCase> primitiveTestCases() {
        return Arrays.asList(
                new PrimitiveTestCase(
                        "string",
                        "test",
                        String.class,
                        "test"
                ),
                new PrimitiveTestCase(
                        "int",
                        42,
                        Integer.class,
                        "42"
                ),
                new PrimitiveTestCase(
                        "long",
                        42L,
                        Long.class,
                        "42"
                ),
                new PrimitiveTestCase(
                        "double",
                        42.0,
                        Double.class,
                        "42.0"
                ),
                new PrimitiveTestCase(
                        "float",
                        42.0f,
                        Float.class,
                        "42.0"
                ),
                new PrimitiveTestCase(
                        "boolean",
                        true,
                        Boolean.class,
                        "true"
                ),
                new PrimitiveTestCase(
                        "char",
                        'a',
                        Character.class,
                        "a"
                ),
                new PrimitiveTestCase(
                        "byte",
                        (byte) 42,
                        Byte.class,
                        "42"
                ),
                new PrimitiveTestCase(
                        "short",
                        (short) 42,
                        Short.class,
                        "42"
                )
        );
    }




}

