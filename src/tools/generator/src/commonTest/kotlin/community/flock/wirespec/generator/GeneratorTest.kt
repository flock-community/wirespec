package community.flock.wirespec.generator

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.Reference.Primitive.Type
import community.flock.wirespec.compiler.utils.NoLogger
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GeneratorTest {

    private val src = """
        |type UUID = String(/^[0-9a-f]{8}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{4}\b-[0-9a-f]{12}$/g)
        |type Name = String(/^[0-9a-zA-Z]{1,50}${'$'}/g)
        |type DutchPostalCode = String(/^([0-9]{4}[A-Z]{2})${'$'}/g)
        |type Date = String(/^([0-9]{2}-[0-9]{2}-20[0-9]{2})${'$'}/g)
        |
        |type Address {
        |  street: Name,
        |  houseNumber: Integer,
        |  postalCode: DutchPostalCode
        |}
        |
        |type PersonA {
        |  uuid: UUID,
        |  firstname: Name,
        |  lastName: Name,
        |  addresses: Address[],
        |  age: Integer
        |}
        |
        |type PersonB {
        |  uuid: UUID,
        |  lastName: Name,
        |  firstname: Name,
        |  age: Integer,
        |  addresses: Address[]
        |}
    """.trimMargin()

    @Test
    fun generateAddress() {
        val ast = parser(src)
        val random = Random(0L)
        val res = ast.generate("Address", random)
        val expect = """{"street":"HQp4YEz0","houseNumber":2133997452,"postalCode":"7542RZ"}"""
        assertEquals(expect, res.toString())
    }

    @Test
    fun generatePrimitive() {
        val ast = parser(src)
        val random = Random(0L)
        val res = ast.generate(Primitive(type = Type.String(), isNullable = false), random)
        val expect = "ZKN8V5p8ktkmmMX"
        assertEquals(expect, res.jsonPrimitive.content)
    }

    @Test
    fun shouldBeEqualWhenSameAttributes() {
        val ast = parser(src)
        val random1 = Random(2L)
        val personA = ast.generate("PersonA", random1)
        val random2 = Random(2L)
        val personB1 = ast.generate("PersonB", random2)
        val personB2 = ast.generate("PersonB", random2)
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

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).getOrElse { e -> error("Cannot parse: ${e.map { it.message }}") }
}
