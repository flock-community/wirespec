import Generator.generate
import arrow.core.getOrElse
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import kotlinx.serialization.json.jsonArray
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GeneratorTest {
    private val src = """
        type UUID /^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}${'$'}/g
        type Name /^[0-9a-zA-Z]{1,50}${'$'}/g
        type DutchPostalCode /^([0-9]{4}[A-Z]{2})${'$'}/g
        type Date /^([0-9]{2}-[0-9]{2}-20[0-9]{2})${'$'}/g
        
        type Address {
          street: Name,
          houseNumber: Integer,
          postalCode: DutchPostalCode
        }
        
        type PersonA {
          uuid: UUID,
          firstname: Name,
          lastName: Name,
          addresses: Address[],
          age: Integer
        }
        
        type PersonB {
          uuid: UUID,
          lastName: Name,
          firstname: Name,
          age: Integer,
          addresses: Address[]
        }
    """.trimIndent()


    private fun parser(source: String) =
        WirespecSpec.parse(source)(noLogger).getOrElse { e -> error("Cannot parser: ${e.map { it.message }}") }

    @Test
    fun generateAdderss() {
        val ast = parser(src)
        val random = Random(0L)
        val res = ast.generate("Address", random)
        val expect = """{"street":"HQp4YEz0","houseNumber":2133997452,"postalCode":"7542RZ"}"""
        assertEquals(expect, res.toString())
    }

    @Test
    fun shouldBeEqualWhenSameAttributes() {
        val ast = parser(src)
        val random1 = Random(2L)
        val personA = generate(ast, "PersonA", random1)
        val random2 = Random(2L)
        val personB1 = generate(ast, "PersonB", random2)
        val personB2 = generate(ast, "PersonB", random2)
        assertEquals(personA, personB1)
        assertNotEquals(personB1, personB2)
    }

    @Test
    fun generateWithExtFun() {
        val ast = parser(src)
        val random = Random(0L)
        val res = ast.generate("Address[]", random)
        assertEquals(5, res.jsonArray.size)
    }

}