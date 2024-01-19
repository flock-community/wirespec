package community.flock.wirespec.convert.avro

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.Endpoint
import community.flock.wirespec.compiler.core.parse.Enum
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.Identifier
import community.flock.wirespec.compiler.core.parse.Node
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Refined
import community.flock.wirespec.compiler.core.parse.Type
import community.flock.wirespec.compiler.core.parse.Union
import community.flock.wirespec.convert.avro.AvroConverter.flatten
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AvroParserTest {

    @Test
    fun testExampleSchema() {
        val resource = Resource("src/commonTest/resources/example.avsc")
            .apply { exists() shouldBe true }

        val schema = AvroParser.parse(resource.readText())
        val ast = schema.flatten()


        ast.map { it.toName() } shouldBe listOf(
            "User",
            "EmailAddress",
            "TwitterAccount",
            "OAuthStatus",
            "ToDoItem",
            "ToDoStatus"
        )


        ast.first() shouldBe Type(
            comment = null,
            identifier = Identifier("User"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = Identifier(value = "id"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = Identifier(value = "username"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = Identifier(value = "passwordHash"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String,
                            isIterable = false,
                            isDictionary = false
                        ),
                        isNullable = false
                    ),
                    Field(
                        identifier = Identifier(value = "signupDate"),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer,
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
            ),
            extends = emptyList()
        )

        ast.last() shouldBe Enum(
            comment = null,
            identifier = Identifier("ToDoStatus"),
            entries = setOf("HIDDEN", "ACTIONABLE", "DONE", "ARCHIVED", "DELETED")
        )

    }
}

private fun Node.toName() = when (this) {
    is Type -> identifier.value
    is Endpoint -> identifier.value
    is Enum -> identifier.value
    is Refined -> identifier.value
    is Channel -> identifier.value
    is Union -> identifier.value
}
