package community.flock.wirespec.convert.avro

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.parse.nodes.Endpoint
import community.flock.wirespec.compiler.core.parse.nodes.Enum
import community.flock.wirespec.compiler.core.parse.nodes.Node
import community.flock.wirespec.compiler.core.parse.nodes.Refined
import community.flock.wirespec.compiler.core.parse.nodes.Type
import community.flock.wirespec.convert.avro.AvroConverter.flatten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvroParserTest {

    @Test
    fun testExampleSchema() {
        val resource = Resource("src/commonTest/resources/example.avsc")
            .apply { assertTrue(exists()) }

        val schema = AvroParser.parse(resource.readText())
        val ast = schema.flatten()

        assertEquals(
            listOf("User", "EmailAddress", "TwitterAccount", "OAuthStatus", "ToDoItem", "ToDoStatus"),
            ast.map { it.toName() }
        )

        assertEquals(
            Type(
                name = "User",
                shape = Type.Shape(
                    value = listOf(
                        Type.Shape.Field(
                            identifier = Type.Shape.Field.Identifier(value = "id"),
                            reference = Type.Shape.Field.Reference.Primitive(
                                type = Type.Shape.Field.Reference.Primitive.Type.Integer,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Type.Shape.Field(
                            identifier = Type.Shape.Field.Identifier(value = "username"),
                            reference = Type.Shape.Field.Reference.Primitive(
                                type = Type.Shape.Field.Reference.Primitive.Type.String,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Type.Shape.Field(
                            identifier = Type.Shape.Field.Identifier(value = "passwordHash"),
                            reference = Type.Shape.Field.Reference.Primitive(
                                type = Type.Shape.Field.Reference.Primitive.Type.String,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Type.Shape.Field(
                            identifier = Type.Shape.Field.Identifier(value = "signupDate"),
                            reference = Type.Shape.Field.Reference.Primitive(
                                type = Type.Shape.Field.Reference.Primitive.Type.Integer,
                                isIterable = false,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Type.Shape.Field(
                            identifier = Type.Shape.Field.Identifier(value = "emailAddresses"),
                            reference = Type.Shape.Field.Reference.Custom(
                                value = "EmailAddress",
                                isIterable = true,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Type.Shape.Field(
                            identifier = Type.Shape.Field.Identifier(value = "twitterAccounts"),
                            reference = Type.Shape.Field.Reference.Custom(
                                value = "TwitterAccount",
                                isIterable = true,
                                isMap = false
                            ),
                            isNullable = false
                        ),
                        Type.Shape.Field(
                            identifier = Type.Shape.Field.Identifier(value = "toDoItems"),
                            reference = Type.Shape.Field.Reference.Custom(
                                value = "ToDoItem",
                                isIterable = true,
                                isMap = false
                            ),
                            isNullable = false
                        )
                    )
                )
            ),
            ast.first()
        )

        assertEquals(
            Enum(
                name = "ToDoStatus",
                entries = setOf("HIDDEN", "ACTIONABLE", "DONE", "ARCHIVED", "DELETED")

            ),
            ast.last()
        )

    }
}

private fun Node.toName() = when(this){
    is Type -> name
    is Endpoint -> name
    is Enum -> name
    is Refined -> name
}