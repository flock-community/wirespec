package community.flock.wirespec.integration.spring.kotlin.configuration

import com.fasterxml.jackson.databind.ObjectMapper
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
                configuration.serializeQuery("param", testCase.value, testCase.type)
            )
        }

        @Test
        fun `test array of primitives`() {
            val colors = listOf("red", "blue", "green")
            assertEquals(
                mapOf("colors" to listOf("red", "blue", "green")),
                configuration.serializeQuery("colors", colors, typeOf<List<String>>())
            )
        }

        @Test
        fun `test object serialization`() {
            val color = Color(100, 200, 150)
            assertEquals(
                mapOf(
                    "r" to listOf("100"),
                    "g" to listOf("200"),
                    "b" to listOf("150")
                ),
                configuration.serializeQuery("color", color, typeOf<Color>())
            )
        }

        @Test
        fun `test object with nullable fields`() {
            val user = User("john", null)
            assertEquals(
                mapOf("name" to listOf("john")),
                configuration.serializeQuery("user", user, typeOf<User>())
            )
        }

        @Test
        fun `test array of objects`() {
            val points = listOf(
                Point(1, 2),
                Point(3, 4)
            )
            assertEquals(
                mapOf(
                    "x" to listOf("1", "3"),
                    "y" to listOf("2", "4")
                ),
                configuration.serializeQuery("points", points, typeOf<List<Point>>())
            )
        }

        @Test
        fun `test single Wirespec Enum`() {
            assertEquals(
                mapOf("status" to listOf("active")),
                configuration.serializeQuery("status", StatusEnum.ACTIVE, typeOf<StatusEnum>())
            )

            assertEquals(
                mapOf("status" to listOf("pending")),
                configuration.serializeQuery("status", StatusEnum.PENDING, typeOf<StatusEnum>())
            )
        }

        @Test
        fun `test array of Wirespec Enum`() {
            val statuses = listOf(StatusEnum.ACTIVE, StatusEnum.INACTIVE)
            assertEquals(
                mapOf("status" to listOf("active", "inactive")),
                configuration.serializeQuery("status", statuses, typeOf<List<StatusEnum>>())
            )
        }

        @Test
        fun `test object containing Wirespec Enum`() {
            val filter = FilterObject(StatusEnum.PENDING, "test")
            assertEquals(
                mapOf(
                    "status" to listOf("pending"),
                    "name" to listOf("test")
                ),
                configuration.serializeQuery("filter", filter, typeOf<FilterObject>())
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
                configuration.deserializeQuery("param", false, testCase.expected, testCase.type)
            )
        }

        @Test
        fun `test primitive nullable when present`() {
            val result = configuration.deserializeQuery<String?>(
                "param",
                true,
                mapOf("param" to listOf("test")),
                typeOf<String?>()
            )
            assertEquals("test", result)
        }

        @Test
        fun `test primitive nullable when absent`() {
            val result = configuration.deserializeQuery<String?>(
                "param",
                true,
                emptyMap(),
                typeOf<String?>()
            )
            assertNull(result)
        }

        @Test
        fun `test array of primitives`() {
            val result = configuration.deserializeQuery<List<String>>(
                "colors",
                false,
                mapOf("colors" to listOf("red", "blue", "green")),
                typeOf<List<String>>()
            )
            assertEquals(listOf("red", "blue", "green"), result)
        }

        @Test
        fun `test object deserialization`() {
            val result = configuration.deserializeQuery<Color>(
                "color",
                false,
                mapOf(
                    "r" to listOf("100"),
                    "g" to listOf("200"),
                    "b" to listOf("150")
                ),
                typeOf<Color>()
            )
            assertEquals(Color(100, 200, 150), result)
        }

        @Test
        fun `test object with nullable fields present`() {
            val result = configuration.deserializeQuery<User>(
                "user",
                false,
                mapOf(
                    "name" to listOf("john"),
                    "age" to listOf("25")
                ),
                typeOf<User>()
            )
            assertEquals(User("john", 25), result)
        }

        @Test
        fun `test object with nullable fields absent`() {
            val result = configuration.deserializeQuery<User>(
                "user",
                false,
                mapOf("name" to listOf("john")),
                typeOf<User>()
            )
            assertEquals(User("john", null), result)
        }

        @Test
        fun `test array of objects`() {
            val result = configuration.deserializeQuery<List<Point>>(
                "points",
                false,
                mapOf(
                    "x" to listOf("1", "3"),
                    "y" to listOf("2", "4")
                ),
                typeOf<List<Point>>()
            )
            assertEquals(
                listOf(
                    Point(1, 2),
                    Point(3, 4)
                ),
                result
            )
        }

        @Test
        fun `test array of objects with missing nullable fields`() {
            val result = configuration.deserializeQuery<List<User>>(
                "users",
                false,
                mapOf(
                    "name" to listOf("john", "jane"),
                    "age" to listOf("25")
                ),
                typeOf<List<User>>()
            )
            assertEquals(
                listOf(
                    User("john", 25),
                    User("jane", null)
                ),
                result
            )
        }

        @Test
        fun `test single Wirespec Enum`() {
            val result = configuration.deserializeQuery<StatusEnum>(
                "status",
                false,
                mapOf("status" to listOf("active")),
                typeOf<StatusEnum>()
            )
            assertEquals(StatusEnum.ACTIVE, result)
        }

        @Test
        fun `test array of Wirespec Enum`() {
            val result = configuration.deserializeQuery<List<StatusEnum>>(
                "status",
                false,
                mapOf("status" to listOf("active", "inactive")),
                typeOf<List<StatusEnum>>()
            )
            assertEquals(listOf(StatusEnum.ACTIVE, StatusEnum.INACTIVE), result)
        }

        @Test
        fun `test object containing Wirespec Enum`() {
            val result = configuration.deserializeQuery<FilterObject>(
                "filter",
                false,
                mapOf(
                    "status" to listOf("pending"),
                    "name" to listOf("test")
                ),
                typeOf<FilterObject>()
            )
            assertEquals(FilterObject(StatusEnum.PENDING, "test"), result)
        }

        @Test
        fun `test missing required primitive throws exception`() {
            assertThrows(IllegalArgumentException::class.java) {
                configuration.deserializeQuery<String>(
                    "param",
                    false,
                    emptyMap(),
                    typeOf<String>()
                )
            }
        }

        @Test
        fun `test missing required object field throws exception`() {
            assertThrows(IllegalArgumentException::class.java) {
                configuration.deserializeQuery<User>(
                    "user",
                    false,
                    mapOf("age" to listOf("25")),  // missing required 'name' field
                    typeOf<User>()
                )
            }
        }

        @Test
        fun `test missing required object field in array throws exception`() {
            assertThrows(IllegalArgumentException::class.java) {
                configuration.deserializeQuery<List<User>>(
                    "users",
                    false,
                    mapOf(
                        "name" to listOf("john"),
                        "age" to listOf("25", "30")  // second item missing required 'name'
                    ),
                    typeOf<List<User>>()
                )
            }
        }

        @Test
        fun `test invalid enum value throws exception`() {
            assertThrows(IllegalArgumentException::class.java) {
                configuration.deserializeQuery<StatusEnum>(
                    "status",
                    false,
                    mapOf("status" to listOf("invalid")),
                    typeOf<StatusEnum>()
                )
            }
        }
    }

    // Test data classes
    data class Color(val r: Int, val g: Int, val b: Int)
    data class User(val name: String, val age: Int?)
    data class Point(val x: Int, val y: Int)
    data class FilterObject(val status: StatusEnum, val name: String)

    // Enum definition
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
        val expected: Map<String, List<String>>
    )

    companion object {
        @JvmStatic
        fun primitiveTestCases() = listOf(
            PrimitiveTestCase(
                name = "string",
                value = "test",
                type = typeOf<String>(),
                expected = mapOf("param" to listOf("test"))
            ),
            PrimitiveTestCase(
                name = "int",
                value = 42,
                type = typeOf<Int>(),
                expected = mapOf("param" to listOf("42"))
            ),
            PrimitiveTestCase(
                name = "long",
                value = 42L,
                type = typeOf<Long>(),
                expected = mapOf("param" to listOf("42"))
            ),
            PrimitiveTestCase(
                name = "double",
                value = 42.0,
                type = typeOf<Double>(),
                expected = mapOf("param" to listOf("42.0"))
            ),
            PrimitiveTestCase(
                name = "float",
                value = 42.0f,
                type = typeOf<Float>(),
                expected = mapOf("param" to listOf("42.0"))
            ),
            PrimitiveTestCase(
                name = "boolean",
                value = true,
                type = typeOf<Boolean>(),
                expected = mapOf("param" to listOf("true"))
            ),
            PrimitiveTestCase(
                name = "char",
                value = 'a',
                type = typeOf<Char>(),
                expected = mapOf("param" to listOf("a"))
            ),
            PrimitiveTestCase(
                name = "byte",
                value = 42.toByte(),
                type = typeOf<Byte>(),
                expected = mapOf("param" to listOf("42"))
            ),
            PrimitiveTestCase(
                name = "short",
                value = 42.toShort(),
                type = typeOf<Short>(),
                expected = mapOf("param" to listOf("42"))
            )
        )
    }
}