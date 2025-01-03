package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.wirespec.integration.spring.kotlin.configuration.WirespecSerializationConfigurationTest.StatusEnum.*
import community.flock.wirespec.kotlin.Wirespec
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class WirespecSerializationConfigurationTest {

    private val configuration = WirespecSerializationConfiguration().wirespecSerialization(ObjectMapper())

    @Nested
    inner class Serialization {
        @ParameterizedTest(name = "test primitive {0}")
        @MethodSource("community.flock.wirespec.integration.spring.kotlin.configuration.WirespecSerializationConfigurationTest#primitiveTestCases")
        fun `test primitive types`(testCase: PrimitiveTestCase) {
            assertEquals(
                testCase.expected,
                configuration.serializeQuery(testCase.value, testCase.type)
            )
        }

        @Test
        fun `test array of primitives`() {
            val colors = listOf("red", "blue", "green")
            assertEquals(
                listOf("red", "blue", "green"),
                configuration.serializeQuery(colors, typeOf<List<String>>())
            )
        }

        @Test
        fun `test single Wirespec Enum`() {
            assertEquals(
                listOf("active"),
                configuration.serializeQuery(ACTIVE, typeOf<StatusEnum>())
            )

            assertEquals(
                listOf("pending"),
                configuration.serializeQuery(PENDING, typeOf<StatusEnum>())
            )
        }

        @Test
        fun `test array of Wirespec Enum`() {
            val statuses = listOf(ACTIVE, INACTIVE)
            assertEquals(
                listOf("active", "inactive"),
                configuration.serializeQuery(statuses, typeOf<List<StatusEnum>>())
            )
        }
    }

    @Nested
    inner class Deserialization {
        @ParameterizedTest(name = "test primitive {0}")
        @MethodSource("community.flock.wirespec.integration.spring.kotlin.configuration.WirespecSerializationConfigurationTest#primitiveTestCases")
        fun `test primitive types`(testCase: PrimitiveTestCase) {
            assertEquals(
                testCase.value,
                configuration.deserializeQuery(testCase.expected, testCase.type)
            )
        }

        @Test
        fun `test array of primitives`() {
            val result = configuration.deserializeQuery<List<String>>(
                listOf("red", "blue", "green"),
                typeOf<List<String>>()
            )
            assertEquals(listOf("red", "blue", "green"), result)
        }

        @Test
        fun `test single Wirespec Enum`() {
            val result = configuration.deserializeQuery<StatusEnum>(
                listOf("active"),
                typeOf<StatusEnum>()
            )
            assertEquals(ACTIVE, result)
        }

        @Test
        fun `test array of Wirespec Enum`() {
            val result = configuration.deserializeQuery<List<StatusEnum>>(
                listOf("active", "inactive"),
                typeOf<List<StatusEnum>>()
            )
            assertEquals(listOf(ACTIVE, INACTIVE), result)
        }

        @Test
        fun `test missing value for primitive throws exception`() {
            assertThrows(IllegalArgumentException::class.java) {
                configuration.deserializeQuery<String>(
                    emptyList(),
                    typeOf<String>()
                )
            }
        }

        @Test
        fun `test invalid enum value throws exception`() {
            assertThrows(IllegalArgumentException::class.java) {
                configuration.deserializeQuery<StatusEnum>(
                    listOf("invalid"),
                    typeOf<StatusEnum>()
                )
            }
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
        val expected: List<String>
    )

    companion object {
        @JvmStatic
        fun primitiveTestCases() = listOf(
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
}