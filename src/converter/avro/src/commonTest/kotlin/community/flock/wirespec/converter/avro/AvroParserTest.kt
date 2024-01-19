package community.flock.wirespec.converter.avro

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.parse.Channel

import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.converter.avro.AvroConverter.flatten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroParserTest {


    @Test()
    fun testCustomer() {
        val resource = Resource("src/commonTest/resources/customer.avsc")
            .apply { assertTrue(exists()) }

        val ast  = AvroParser.parse(resource.readText())

    }

    @Test()
    fun testUnionSimple() {
        val resource = Resource("src/commonTest/resources/union_simple.avsc")
            .apply { assertTrue(exists()) }

        try {
            AvroParser.parse(resource.readText())
            assertEquals(true, false)
        } catch (e:Exception){
            assertEquals("Cannot have multiple SimpleTypes in Union", e.message)
        }
    }


    @Test()
    fun testUnionComplex() {
        val resource = Resource("src/commonTest/resources/union_complex.avsc")
            .apply { assertTrue(exists()) }

        try {
            AvroParser.parse(resource.readText())
        } catch (e:Exception){
            assertEquals("Cannot have multiple SimpleTypes in Union", e.message)
        }
    }

    @Test
    fun testExampleSchema() {
        val resource = Resource("src/commonTest/resources/example.avsc")
            .apply { assertTrue(exists()) }

        val ast = AvroParser.parse(resource.readText())

        assertEquals(
            listOf("User", "EmailAddress", "TwitterAccount", "OAuthStatus", "ToDoItem", "ToDoStatus", "User"),
            ast.filterIsInstance<Definition>().map { it.identifier.value }
        )

        assertEquals(
            Type(
                comment = null,
                identifier = Identifier("User"),
                extends = emptyList(),
                shape = Type.Shape(
                    value = listOf(
                        Field(
                            identifier = Identifier(value = "id"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer,
                                origin = "int",
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "username"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.String,
                                origin = "string",
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "passwordHash"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.String,
                                origin = "string",
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "signupDate"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer,
                                origin = "long",
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "emailAddresses"),
                            reference = Reference.Custom(
                                value = "EmailAddress",
                                isIterable = true,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "twitterAccounts"),
                            reference = Reference.Custom(
                                value = "TwitterAccount",
                                isIterable = true,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = Identifier(value = "toDoItems"),
                            reference = Reference.Custom(
                                value = "ToDoItem",
                                isIterable = true,
                                isDictionary = false
                            ),
                            isNullable = false
                        )
                    )
                )
            ),
            ast.first()
        )

        assertEquals(
            Channel(
                comment = null,
                identifier = Identifier("User"),
                isNullable = false,
                reference = Reference.Custom(
                    value = "User",
                    isIterable = false,
                )
            ),
            ast.last()
        )

    }
}
