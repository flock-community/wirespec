package community.flock.wirespec.integration.jackson.kotlin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import community.flock.wirespec.integration.jackson.kotlin.generated.Todo
import community.flock.wirespec.integration.jackson.kotlin.generated.TodoCategory
import community.flock.wirespec.integration.jackson.kotlin.generated.TodoId
import kotlin.test.Test
import kotlin.test.assertEquals

class WirespecModuleKotlinTest {

    private val todo = Todo(
        id = TodoId("123"),
        name = "Do It now",
        final = false,
        category = TodoCategory.LIFE
    )

    private val json = "{\"id\":\"123\",\"name\":\"Do It now\",\"final\":false,\"category\":\"LIFE\"}"

    private val objectMapper: ObjectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModules(WirespecModuleKotlin())

    @Test
    fun serializeRefined(){
        val res = objectMapper.writeValueAsString(todo)
        assertEquals(json, res)
    }

    @Test
    fun deserializeRefined(){
        val res = objectMapper.readValue<Todo>(json)
        assertEquals(todo, res)
    }
}
