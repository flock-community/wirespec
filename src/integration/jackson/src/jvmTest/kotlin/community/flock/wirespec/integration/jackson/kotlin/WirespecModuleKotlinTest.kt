package community.flock.wirespec.integration.jackson.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import community.flock.wirespec.integration.jackson.kotlin.generated.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

class WirespecModuleKotlinTest {

    private val todo = Todo(
        id = TodoId("123"),
        name = "Do It now",
        final = false,
        category = TodoCategory.LIFE,
        eMail = "test@wirespec.io",
    )

    private val json = "{\"id\":\"123\",\"name\":\"Do It now\",\"final\":false,\"category\":\"LIFE\",\"eMail\":\"test@wirespec.io\"}"

    private val typeWithAllRefined = TypeWithAllRefined(
        stringRefinedRegex = StringRefinedRegex("string refined regex"),
        stringRefined = StringRefined("string refined"),
        intRefinedNoBound = IntRefinedNoBound(1L),
        intRefinedLowerBound = IntRefinedLowerBound(2L),
        intRefinedUpperound = IntRefinedUpperound(3L),
        intRefinedLowerAndUpper = IntRefinedLowerAndUpper(4L),
        numberRefinedNoBound = NumberRefinedNoBound(1.0),
        numberRefinedLowerBound = NumberRefinedLowerBound(2.0),
        numberRefinedUpperound = NumberRefinedUpperound(3.0),
        numberRefinedLowerAndUpper = NumberRefinedLowerAndUpper(4.0)
    )

    private val typeJson = "{\"stringRefinedRegex\":\"string refined regex\",\"stringRefined\":\"string refined\",\"intRefinedNoBound\":1,\"intRefinedLowerBound\":2,\"intRefinedUpperound\":3,\"intRefinedLowerAndUpper\":4,\"numberRefinedNoBound\":1.0,\"numberRefinedLowerBound\":2.0,\"numberRefinedUpperound\":3.0,\"numberRefinedLowerAndUpper\":4.0}"

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModules(WirespecModuleKotlin())

    @Test
    fun serializeRefined() {
        val res = objectMapper.writeValueAsString(todo)
        assertEquals(json, res)
    }

    @Test
    fun deserializeRefined() {
        val res = objectMapper.readValue<Todo>(json)
        assertEquals(todo, res)
    }

    @Test
    fun serializeRefined2() {
        val res = objectMapper.writeValueAsString(typeWithAllRefined)
        assertEquals(typeJson, res)
    }

    @Test
    fun deserializeRefined2() {
        val res = objectMapper.readValue<TypeWithAllRefined>(typeJson)
        assertEquals(typeWithAllRefined, res)
    }
}
