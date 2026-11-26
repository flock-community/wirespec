package community.flock.wirespec.integration.kotlinxserialization

import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Dog
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LiteralSerializationTest {

    @Test
    fun `Should write the literal of a field that was not set`() {
        assertEquals("""{"kind":"dog","name":"Rex"}""", Json.encodeToString(Dog(name = "Rex")))
    }

    @Test
    fun `Should read a model without its literal`() {
        assertEquals(Dog(name = "Rex"), Json.decodeFromString<Dog>("""{"name":"Rex"}"""))
    }

    @Test
    fun `Should report another value than the literal`() {
        assertEquals(listOf("kind"), Json.decodeFromString<Dog>("""{"kind":"cat","name":"Rex"}""").validate())
    }
}
