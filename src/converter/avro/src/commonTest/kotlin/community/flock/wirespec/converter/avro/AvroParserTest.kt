package community.flock.wirespec.converter.avro

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroParserTest {


    @Test()
    fun testCustomer() {
        val resource = Resource("src/commonTest/resources/customer.avsc")
            .apply { assertTrue(exists()) }

        val ast = AvroParser.parse(resource.readText())

    }

    @Test()
    fun testUnionSimple() {
        val resource = Resource("src/commonTest/resources/union_simple.avsc")
            .apply { assertTrue(exists()) }

        try {
            AvroParser.parse(resource.readText())
            assertEquals(true, false)
        } catch (e: Exception) {
            assertEquals("Cannot have multiple SimpleTypes in Union", e.message)
        }
    }


    @Test()
    fun testUnionComplex() {
        val resource = Resource("src/commonTest/resources/union_complex.avsc")
            .apply { assertTrue(exists()) }

        try {
            AvroParser.parse(resource.readText())
        } catch (e: Exception) {
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
                identifier = DefinitionIdentifier("User"),
                extends = emptyList(),
                shape = Type.Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32),
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("passwordHash"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.String,
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("signupDate"),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64),
                                isIterable = false,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("emailAddresses"),
                            reference = Reference.Custom(
                                "EmailAddress",
                                isIterable = true,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("twitterAccounts"),
                            reference = Reference.Custom(
                                "TwitterAccount",
                                isIterable = true,
                                isDictionary = false
                            ),
                            isNullable = false
                        ),
                        Field(
                            identifier = FieldIdentifier("toDoItems"),
                            reference = Reference.Custom(
                                "ToDoItem",
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
                identifier = DefinitionIdentifier("User"),
                isNullable = false,
                reference = Reference.Custom(
                    "User",
                    isIterable = false,
                )
            ),
            ast.last()
        )

    }
}
