package community.flock.wirespec.generator

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive
import community.flock.wirespec.compiler.core.parse.ast.Reference.Primitive.Type
import community.flock.wirespec.compiler.utils.NoLogger
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GeneratorTest {

    private val src =
        // language=ws
        """
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

    private val endpointSrc = src +
        // language=ws
        """
        |
        |type NotFound {
        |  message: String
        |}
        |
        |endpoint GetPerson GET /person/{id: UUID} -> {
        |  200 -> PersonA
        |  404 -> NotFound
        |}
        |
        |endpoint PostPerson POST PersonA /person -> {
        |  201 -> PersonA
        |}
        |
        |endpoint SearchPersons GET /persons ?{name: Name} #{auth: Name} -> {
        |  200 -> PersonA
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
        val res = ast.generate(Primitive(type = Type.String(null), isNullable = false), random)
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

    @Test
    fun generateRequestWithNoBody() {
        val ast = parser(endpointSrc)
        val random = Random(0L)
        val request = ast.generateRequest("GetPerson", random)

        assertEquals("GET", request["method"]!!.jsonPrimitive.content)
        assertTrue(request["path"]!!.jsonObject.containsKey("id"))
        assertTrue(request["queries"]!!.jsonObject.isEmpty())
        assertTrue(request["headers"]!!.jsonObject.isEmpty())
        assertEquals(JsonNull, request["body"])
    }

    @Test
    fun generateRequestWithBody() {
        val ast = parser(endpointSrc)
        val random = Random(0L)
        val request = ast.generateRequest("PostPerson", random)

        assertEquals("POST", request["method"]!!.jsonPrimitive.content)
        assertTrue(request["path"]!!.jsonObject.isEmpty())
        assertTrue(request["body"]!!.jsonObject.containsKey("uuid"))
        assertTrue(request["body"]!!.jsonObject.containsKey("firstname"))
    }

    @Test
    fun generateRequestWithQueryAndHeader() {
        val ast = parser(endpointSrc)
        val random = Random(0L)
        val request = ast.generateRequest("SearchPersons", random)

        assertEquals("GET", request["method"]!!.jsonPrimitive.content)
        assertTrue(request["queries"]!!.jsonObject.containsKey("name"))
        assertTrue(request["headers"]!!.jsonObject.containsKey("auth"))
        assertEquals(JsonNull, request["body"])
    }

    @Test
    fun generateResponseWithSpecificStatus() {
        val ast = parser(endpointSrc)
        val random = Random(0L)
        val response = ast.generateResponse("GetPerson", "200", random)

        assertEquals(200, response["status"]!!.jsonPrimitive.content.toInt())
        assertTrue(response["headers"]!!.jsonObject.isEmpty())
        assertTrue(response["body"]!!.jsonObject.containsKey("uuid"))
        assertTrue(response["body"]!!.jsonObject.containsKey("firstname"))
        assertTrue(response["body"]!!.jsonObject.containsKey("age"))
    }

    @Test
    fun generateResponseNotFound() {
        val ast = parser(endpointSrc)
        val random = Random(0L)
        val response = ast.generateResponse("GetPerson", "404", random)

        assertEquals(404, response["status"]!!.jsonPrimitive.content.toInt())
        assertTrue(response["body"]!!.jsonObject.containsKey("message"))
    }

    @Test
    fun generateResponsePicksRandomStatus() {
        val ast = parser(endpointSrc)
        // GetPerson has two responses: 200 and 404 — verify both can be picked
        val statuses = (0..99).map { seed ->
            ast.generateResponse("GetPerson", Random(seed.toLong()))["status"]!!.jsonPrimitive.content.toInt()
        }.toSet()
        assertTrue(statuses.contains(200))
        assertTrue(statuses.contains(404))
    }

    private fun parser(source: String) = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrElse { e -> error("Cannot parse: ${e.map { it.message }}") }
}
