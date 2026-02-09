package community.flock.wirespec.kotlin.serde

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultPathSerializationTest {

    private val serde = DefaultPathSerialization()

    @Test
    fun `should serialize primitive types correctly`() {
        primitiveTestCases().forEach { testCase ->
            assertEquals(
                testCase.expected,
                serde.serializePath(testCase.value, testCase.type),
                "Failed to serialize ${testCase.name}: expected ${testCase.expected}, but got ${serde.serializePath(testCase.value, testCase.type)}",
            )
        }
    }

    @Test
    fun `should serialize single Wirespec Enum correctly`() {
        assertEquals(
            "active",
            serde.serializePath(StatusEnum.ACTIVE, typeOf<StatusEnum>()),
        )

        assertEquals(
            "pending",
            serde.serializePath(StatusEnum.PENDING, typeOf<StatusEnum>()),
        )
    }

    @Test
    fun `should deserialize primitive types correctly`() {
        primitiveTestCases().forEach { testCase ->
            assertEquals(
                testCase.value,
                serde.deserializePath(testCase.expected, testCase.type),
                "Failed to deserialize ${testCase.name}: expected ${testCase.value}",
            )
        }
    }

    @Test
    fun `should deserialize single Wirespec Enum correctly`() {
        val result = serde.deserializePath<StatusEnum>(
            "active",
            typeOf<StatusEnum>(),
        )
        assertEquals(StatusEnum.ACTIVE, result)
    }

    @Test
    fun `should serialize Wirespec Refined String correctly`() {
        assertEquals("test-uuid", serde.serializePath(StringRefined("test-uuid"), typeOf<StringRefined>()))
    }

    @Test
    fun `should serialize Wirespec Refined Long correctly`() {
        assertEquals("42", serde.serializePath(LongRefined(42L), typeOf<LongRefined>()))
    }

    @Test
    fun `should serialize Wirespec Refined Double correctly`() {
        assertEquals("3.14", serde.serializePath(DoubleRefined(3.14), typeOf<DoubleRefined>()))
    }

    @Test
    fun `should deserialize Wirespec Refined String correctly`() {
        assertEquals(StringRefined("test-uuid"), serde.deserializePath<StringRefined>("test-uuid", typeOf<StringRefined>()))
    }

    @Test
    fun `should deserialize Wirespec Refined Long correctly`() {
        assertEquals(LongRefined(42L), serde.deserializePath<LongRefined>("42", typeOf<LongRefined>()))
    }

    @Test
    fun `should deserialize Wirespec Refined Double correctly`() {
        assertEquals(DoubleRefined(3.14), serde.deserializePath<DoubleRefined>("3.14", typeOf<DoubleRefined>()))
    }

    @Test
    fun `should throw exception when deserializing invalid enum value`() {
        assertFailsWith<IllegalStateException> {
            serde.deserializePath<StatusEnum>(
                "invalid",
                typeOf<StatusEnum>(),
            )
        }
    }

    data class StringRefined(override val value: String) : Wirespec.Refined<String>
    data class LongRefined(override val value: Long) : Wirespec.Refined<Long>
    data class DoubleRefined(override val value: Double) : Wirespec.Refined<Double>

    enum class StatusEnum(override val label: String) : Wirespec.Enum {
        ACTIVE("active"),
        PENDING("pending"),
        INACTIVE("inactive"),
        ;

        override fun toString(): String = label
    }

    data class PrimitiveTestCase(
        val name: String,
        val value: Any,
        val type: KType,
        val expected: String,
    )

    private fun primitiveTestCases() = listOf(
        PrimitiveTestCase(
            name = "string",
            value = "test",
            type = typeOf<String>(),
            expected = "test",
        ),
        PrimitiveTestCase(
            name = "int",
            value = 42,
            type = typeOf<Int>(),
            expected = "42",
        ),
        PrimitiveTestCase(
            name = "long",
            value = 42L,
            type = typeOf<Long>(),
            expected = "42",
        ),
        PrimitiveTestCase(
            name = "double",
            value = 42.0,
            type = typeOf<Double>(),
            expected = "42.0",
        ),
        PrimitiveTestCase(
            name = "float",
            value = 42.0f,
            type = typeOf<Float>(),
            expected = "42.0",
        ),
        PrimitiveTestCase(
            name = "boolean",
            value = true,
            type = typeOf<Boolean>(),
            expected = "true",
        ),
        PrimitiveTestCase(
            name = "char",
            value = 'a',
            type = typeOf<Char>(),
            expected = "a",
        ),
        PrimitiveTestCase(
            name = "byte",
            value = 42.toByte(),
            type = typeOf<Byte>(),
            expected = "42",
        ),
        PrimitiveTestCase(
            name = "short",
            value = 42.toShort(),
            type = typeOf<Short>(),
            expected = "42",
        ),
    )
}
