package community.flock.wirespec.integration.kotlinxserialization

import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Priority
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Progress
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Status
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Todo
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.TodoId
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlin.test.Test

class KotlinxSerializationRoundTripTest {

    private val todo = Todo(
        id = TodoId("1"),
        name = "Flatten refined types",
        priority = Priority(3L),
        progress = Progress(0.5),
        status = Status.OPEN,
        parent = null,
        related = listOf(TodoId("2"), TodoId("3")),
    )

    private val json =
        //language=json
        """
        |{
        |  "id": "1",
        |  "name": "Flatten refined types",
        |  "priority": 3,
        |  "progress": 0.5,
        |  "status": "OPEN",
        |  "parent": null,
        |  "related": ["2", "3"]
        |}
        """.trimMargin()

    @Test
    fun `Should serialize refined types as their underlying value`() {
        Json.encodeToString(Todo.serializer(), todo) shouldEqualJson json
    }

    @Test
    fun `Should deserialize refined types from their underlying value`() {
        Json.decodeFromString(Todo.serializer(), json) shouldBe todo
    }
}
