package community.flock.wirespec.integration.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlin.test.Test
import kotlin.test.assertEquals

class WirespecModuleTest {


    @Test
    fun serializeRefined(){

        val todo = Todo(
            id = TodoId("123"),
            name = "Do It now",
            done = false
        )

        val objectMapper = ObjectMapper()
            .registerModules(WirespecModule())

        val json = objectMapper.writeValueAsString(todo)
        val expected = "{\"id\":\"123\",\"name\":\"Do It now\",\"done\":false}"
        assertEquals(expected, json)

    }

    @Test
    fun deserializeRefined(){

        val json = """
            {
                "id":"123456",
                "name":"Do It now",
                "done":false
            }
        """.trimIndent()

        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModules(WirespecModule())

        val todo = objectMapper.readValue<Todo>(json)

        println(todo)

    }
}