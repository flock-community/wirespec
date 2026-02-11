package community.flock.wirespec.java.serde;

import community.flock.wirespec.java.Wirespec;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultPathSerializationTest {

    private final DefaultPathSerialization serde = new DefaultPathSerialization() {};

    record StringRefined(String value) implements Wirespec.Refined<String> {
        @Override public String value() { return value; }
    }

    record LongRefined(Long value) implements Wirespec.Refined<Long> {
        @Override public Long value() { return value; }
    }

    record DoubleRefined(Double value) implements Wirespec.Refined<Double> {
        @Override public Double value() { return value; }
    }

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

    @Test
    public void shouldSerializeRefinedTypesCorrectly() {
        assertEquals("test-uuid", serde.serializePath(new StringRefined("test-uuid"), StringRefined.class));
        assertEquals("42", serde.serializePath(new LongRefined(42L), LongRefined.class));
        assertEquals("3.14", serde.serializePath(new DoubleRefined(3.14), DoubleRefined.class));
    }

    @Test
    public void shouldDeserializeRefinedTypesCorrectly() {
        assertEquals(new StringRefined("test-uuid"), serde.deserializePath("test-uuid", StringRefined.class));
        assertEquals(new LongRefined(42L), serde.deserializePath("42", LongRefined.class));
        assertEquals(new DoubleRefined(3.14), serde.deserializePath("3.14", DoubleRefined.class));
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

