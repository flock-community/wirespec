package community.flock.wirespec.integration.kotlinxserialization

import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Account
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Animal
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Bird
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Cat
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Owner
import community.flock.wirespec.integration.kotlinxserialization.kotlin.generated.model.Token
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnionSerializationTest {

    private val owner = Owner(name = "Ann", animal = Cat(lives = 9))
    private val ownerJson = """{"name":"Ann","animal":{"kind":"cat","lives":9}}"""
    private val birdJson = """{"kind":"bird","name":"Tweety"}"""

    @Test
    fun `Should write a member with its own field and no type`() {
        assertEquals(ownerJson, Json.encodeToString(owner))
        assertEquals(birdJson, Json.encodeToString<Animal>(Bird(name = "Tweety")))
    }

    @Test
    fun `Should read the member its discriminator names`() {
        assertEquals(owner, Json.decodeFromString<Owner>(ownerJson))
        assertEquals(Bird(name = "Tweety"), Json.decodeFromString<Animal>(birdJson))
    }

    @Test
    fun `Should reject a literal no member has`() {
        assertFailsWith<SerializationException> { Json.decodeFromString<Animal>("""{"kind":"fish","name":"Nemo"}""") }
    }

    @Test
    fun `Should write a union without a discriminator without a type`() {
        assertEquals("""{"token":"abc"}""", Json.encodeToString<Account>(Token(token = "abc")))
    }

    @Test
    fun `Should not read a union without a discriminator`() {
        assertFailsWith<SerializationException> { Json.decodeFromString<Account>("""{"token":"abc"}""") }
    }
}
