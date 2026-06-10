package community.flock.wirespec.integration.jackson.v3.kotlin

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import community.flock.wirespec.integration.jackson.kotlin.generated.model.IntRefinedLowerAndUpper
import community.flock.wirespec.integration.jackson.kotlin.generated.model.IntRefinedLowerBound
import community.flock.wirespec.integration.jackson.kotlin.generated.model.IntRefinedNoBound
import community.flock.wirespec.integration.jackson.kotlin.generated.model.IntRefinedUpperBound
import community.flock.wirespec.integration.jackson.kotlin.generated.model.NumberRefinedLowerAndUpper
import community.flock.wirespec.integration.jackson.kotlin.generated.model.NumberRefinedLowerBound
import community.flock.wirespec.integration.jackson.kotlin.generated.model.NumberRefinedNoBound
import community.flock.wirespec.integration.jackson.kotlin.generated.model.NumberRefinedUpperBound
import community.flock.wirespec.integration.jackson.kotlin.generated.model.StringRefined
import community.flock.wirespec.integration.jackson.kotlin.generated.model.StringRefinedRegex
import community.flock.wirespec.integration.jackson.kotlin.generated.model.Todo
import community.flock.wirespec.integration.jackson.kotlin.generated.model.TodoCategory
import community.flock.wirespec.integration.jackson.kotlin.generated.model.TodoId
import community.flock.wirespec.integration.jackson.kotlin.generated.model.TypeWithAllRefined
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test

class WirespecModuleKotlinTest {

    private val todo = Todo(
        id = TodoId("123"),
        name = "Do It now",
        final = false,
        category = TodoCategory.LIFE,
        eMail = "test@wirespec.io",
    )

    private val json =
        //language=json
        """
        |{
        |  "id": "123",
        |  "name": "Do It now",
        |  "final": false,
        |  "category": "LIFE",
        |  "eMail": "test@wirespec.io"
        |}
        """.trimMargin()

    private val typeWithAllRefined = TypeWithAllRefined(
        stringRefinedRegex = StringRefinedRegex("string refined regex"),
        stringRefined = StringRefined("string refined"),
        intRefinedNoBound = IntRefinedNoBound(1L),
        intRefinedLowerBound = IntRefinedLowerBound(2L),
        intRefinedUpperBound = IntRefinedUpperBound(3L),
        intRefinedLowerAndUpper = IntRefinedLowerAndUpper(4L),
        numberRefinedNoBound = NumberRefinedNoBound(1.0),
        numberRefinedLowerBound = NumberRefinedLowerBound(2.0),
        numberRefinedUpperBound = NumberRefinedUpperBound(3.0),
        numberRefinedLowerAndUpper = NumberRefinedLowerAndUpper(4.0),
    )

    private val typeJson =
        //language=json
        """
        |{
        |  "stringRefinedRegex": "string refined regex",
        |  "stringRefined": "string refined",
        |  "intRefinedNoBound": 1,
        |  "intRefinedLowerBound": 2,
        |  "intRefinedUpperBound": 3,
        |  "intRefinedLowerAndUpper": 4,
        |  "numberRefinedNoBound": 1.0,
        |  "numberRefinedLowerBound": 2.0,
        |  "numberRefinedUpperBound": 3.0,
        |  "numberRefinedLowerAndUpper": 4.0
        |}
        """.trimMargin()

    // Jackson 3 mappers are immutable: field visibility is configured on the builder
    // (a v3 module cannot mutate it). WirespecSerialization applies the same config.
    private val objectMapper: ObjectMapper = jsonMapper {
        addModule(kotlinModule())
        addModule(WirespecModuleKotlin())
        propertyNamingStrategy(KotlinReservedKeywordNamingStrategy())
        changeDefaultVisibility {
            it
                .withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        }
    }

    @Test
    fun serializeRefined() {
        val res = objectMapper.writeValueAsString(todo)
        res shouldEqualJson json
    }

    @Test
    fun deserializeRefined() {
        val res = objectMapper.readValue<Todo>(json)
        todo shouldBe res
    }

    @Test
    fun serializeRefined2() {
        val res = objectMapper.writeValueAsString(typeWithAllRefined)
        res shouldEqualJson typeJson
    }

    @Test
    fun deserializeRefined2() {
        val res = objectMapper.readValue<TypeWithAllRefined>(typeJson)
        res shouldBe typeWithAllRefined
    }
}
