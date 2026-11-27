package community.flock.wirespec.integration.jackson.extension

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import community.flock.wirespec.integration.jackson.kotlin.generated.model.Animal
import community.flock.wirespec.integration.jackson.kotlin.generated.model.Bird
import community.flock.wirespec.integration.jackson.kotlin.generated.model.Cat
import community.flock.wirespec.integration.jackson.kotlin.generated.model.Owner
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import kotlin.test.Test

/** The Kotlin models of `union.ws`, read and written by their Jackson annotations alone, without the Wirespec module. */
class JacksonDiscriminatorKotlinTest {

    private val v2 = com.fasterxml.jackson.databind.ObjectMapper().registerKotlinModule()
    private val v3 = jsonMapper { addModule(kotlinModule()) }

    private val owner = Owner(name = "Ann", animal = Cat(lives = 9))
    private val ownerJson = """{"name":"Ann","animal":{"kind":"cat","lives":9}}"""
    private val birdJson = """{"kind":"bird","name":"Tweety"}"""
    private val fishJson = """{"kind":"fish","name":"Nemo"}"""

    @Test
    fun `Jackson 2 writes a member with its own field and nothing else`() {
        v2.writeValueAsString(owner) shouldEqualJson ownerJson
        v2.writeValueAsString(Bird(name = "Tweety")) shouldEqualJson birdJson
    }

    @Test
    fun `Jackson 2 reads the member its discriminator names`() {
        v2.readValue(ownerJson, Owner::class.java) shouldBe owner
        v2.readValue(birdJson, Animal::class.java) shouldBe Bird(name = "Tweety")
    }

    @Test
    fun `Jackson 2 rejects a literal no member has`() {
        shouldThrowAny { v2.readValue(fishJson, Animal::class.java) }
    }

    @Test
    fun `Jackson 3 writes a member with its own field and nothing else`() {
        v3.writeValueAsString(owner) shouldEqualJson ownerJson
        v3.writeValueAsString(Bird(name = "Tweety")) shouldEqualJson birdJson
    }

    @Test
    fun `Jackson 3 reads the member its discriminator names`() {
        v3.readValue(ownerJson, Owner::class.java) shouldBe owner
        v3.readValue(birdJson, Animal::class.java) shouldBe Bird(name = "Tweety")
    }

    @Test
    fun `Jackson 3 rejects a literal no member has`() {
        shouldThrowAny { v3.readValue(fishJson, Animal::class.java) }
    }
}
