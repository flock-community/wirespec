package community.flock.wirespec.kotlin.serde

import community.flock.wirespec.kotlin.Wirespec
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import kotlin.test.*

class DefaultQueryParamSerdeTest {

    private val serde = DefaultQueryParamSerde()

    @Test
    fun `should serialize primitive types correctly`() {
        primitiveTestCases().forEach { testCase ->
            assertEquals(
                testCase.expected,
                serde.serializeQuery(testCase.value, testCase.type),
                "Failed to serialize ${testCase.name}: expected ${testCase.expected}, but got ${serde.serializeQuery(testCase.value, testCase.type)}"
            )
        }
    }

    @Test
    fun `should serialize array of primitives correctly`() {
        val colors = listOf("red", "blue", "green")
        assertEquals(
            listOf("red", "blue", "green"),
            serde.serializeQuery(colors, typeOf<List<String>>())
        )
    }

    @Test
    fun `should serialize single Wirespec Enum correctly`() {
        assertEquals(
            listOf("active"),
            serde.serializeQuery(StatusEnum.ACTIVE, typeOf<StatusEnum>())
        )

        assertEquals(
            listOf("pending"),
            serde.serializeQuery(StatusEnum.PENDING, typeOf<StatusEnum>())
        )
    }

    @Test
    fun `should serialize array of Wirespec Enum correctly`() {
        val statuses = listOf(StatusEnum.ACTIVE, StatusEnum.INACTIVE)
        assertEquals(
            listOf("active", "inactive"),
            serde.serializeQuery(statuses, typeOf<List<StatusEnum>>())
        )
    }

    @Test
    fun `should deserialize primitive types correctly`() {
        primitiveTestCases().forEach { testCase ->
            assertEquals(
                testCase.value,
                serde.deserializeQuery(testCase.expected, testCase.type),
                "Failed to deserialize ${testCase.name}: expected ${testCase.value}"
            )
        }
    }

    @Test
    fun `should deserialize array of primitives correctly`() {
        val result = serde.deserializeQuery<List<String>>(
            listOf("red", "blue", "green"),
            typeOf<List<String>>()
        )
        assertEquals(listOf("red", "blue", "green"), result)
    }

    @Test
    fun `should deserialize single Wirespec Enum correctly`() {
        val result = serde.deserializeQuery<StatusEnum>(
            listOf("active"),
            typeOf<StatusEnum>()
        )
        assertEquals(StatusEnum.ACTIVE, result)
    }

    @Test
    fun `should deserialize array of Wirespec Enum correctly`() {
        val result = serde.deserializeQuery<List<StatusEnum>>(
            listOf("active", "inactive"),
            typeOf<List<StatusEnum>>()
        )
        assertEquals(listOf(StatusEnum.ACTIVE, StatusEnum.INACTIVE), result)
    }

    @Test
    fun `should throw exception when deserializing missing primitive value`() {
        assertFailsWith<IllegalArgumentException> {
            serde.deserializeQuery<String>(
                emptyList(),
                typeOf<String>()
            )
        }
    }

    @Test
    fun `should throw exception when deserializing invalid enum value`() {
        assertFailsWith<IllegalArgumentException> {
            serde.deserializeQuery<StatusEnum>(
                listOf("invalid"),
                typeOf<StatusEnum>()
            )
        }
    }

    enum class StatusEnum(override val label: String) : Wirespec.Enum {
        ACTIVE("active"),
        PENDING("pending"),
        INACTIVE("inactive");

        override fun toString(): String {
            return label
        }
    }

    data class PrimitiveTestCase(
        val name: String,
        val value: Any,
        val type: KType,
        val expected: List<String>,
    )


    private fun primitiveTestCases() = listOf(
        PrimitiveTestCase(
            name = "string",
            value = "test",
            type = typeOf<String>(),
            expected = listOf("test")
        ),
        PrimitiveTestCase(
            name = "int",
            value = 42,
            type = typeOf<Int>(),
            expected = listOf("42")
        ),
        PrimitiveTestCase(
            name = "long",
            value = 42L,
            type = typeOf<Long>(),
            expected = listOf("42")
        ),
        PrimitiveTestCase(
            name = "double",
            value = 42.0,
            type = typeOf<Double>(),
            expected = listOf("42.0")
        ),
        PrimitiveTestCase(
            name = "float",
            value = 42.0f,
            type = typeOf<Float>(),
            expected = listOf("42.0")
        ),
        PrimitiveTestCase(
            name = "boolean",
            value = true,
            type = typeOf<Boolean>(),
            expected = listOf("true")
        ),
        PrimitiveTestCase(
            name = "char",
            value = 'a',
            type = typeOf<Char>(),
            expected = listOf("a")
        ),
        PrimitiveTestCase(
            name = "byte",
            value = 42.toByte(),
            type = typeOf<Byte>(),
            expected = listOf("42")
        ),
        PrimitiveTestCase(
            name = "short",
            value = 42.toShort(),
            type = typeOf<Short>(),
            expected = listOf("42")
        )
    )
}