package community.flock.wirespec.integration.kotlinxserialization

import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Priority
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Progress
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Todo
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.TodoId
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefinedValueClassSerializationTest {

    private val todo = Todo(
        id = TodoId("1"),
        priority = Priority(3),
        progress = Progress(0.5),
        parent = null,
        related = listOf(TodoId("2"), TodoId("3")),
    )

    private val json = """{"id":"1","priority":3,"progress":0.5,"parent":null,"related":["2","3"]}"""

    @Test
    fun `Should emit refined types as value classes`() {
        listOf(TodoId::class, Priority::class, Progress::class).forEach {
            assertTrue(it.isValue, "${it.simpleName} should be a value class")
        }
    }

    @Test
    fun `Should serialize a refined type as its bare value`() {
        assertEquals("\"1\"", Json.encodeToString(TodoId("1")))
        assertEquals("3", Json.encodeToString(Priority(3)))
        assertEquals("0.5", Json.encodeToString(Progress(0.5)))
    }

    @Test
    fun `Should flatten refined fields when serializing a record`() {
        assertEquals(json, Json.encodeToString(todo))
    }

    @Test
    fun `Should expand flattened values into refined fields when deserializing a record`() {
        assertEquals(todo, Json.decodeFromString<Todo>(json))
    }
}
