package community.flock.wirespec.converter.avro

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.parse.Channel
import community.flock.wirespec.compiler.core.parse.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.Field
import community.flock.wirespec.compiler.core.parse.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.Reference
import community.flock.wirespec.compiler.core.parse.Type
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

class AvroParserTest {

    @Test
    fun testCustomer() {
        val path = Path("src/commonTest/resources/customer.avsc")
        val resource = SystemFileSystem.source(path).buffered().readString()

        AvroParser.parse(ModuleContent(FileUri("test.ws"), resource), true)
    }

    @Test
    fun testUnionSimple() {
        val path = Path("src/commonTest/resources/union_simple.avsc")
        val resource = SystemFileSystem.source(path).buffered().readString()

        shouldThrow<Exception> {
            AvroParser.parse(ModuleContent(FileUri("test.ws"), resource), true)
        }.message shouldBe "Cannot have multiple SimpleTypes in Union"
    }

    @Test
    fun testUnionComplex() {
        val path = Path("src/commonTest/resources/union_complex.avsc")
        val resource = SystemFileSystem.source(path).buffered().readString()

        shouldNotThrow<Exception> {
            AvroParser.parse(ModuleContent(FileUri("test.ws"), resource), true)
        }
    }

    @Test
    fun testExampleSchema() {
        val path = Path("src/commonTest/resources/example.avsc")
        val resource = SystemFileSystem.source(path).buffered().readString()

        val ast = AvroParser.parse(ModuleContent(FileUri("test.ws"), resource), true)

        assertEquals(
            listOf("User", "EmailAddress", "TwitterAccount", "OAuthStatus", "ToDoItem", "ToDoStatus", "User"),
            ast.modules.flatMap { it.statements }.toList().map { it.identifier.value },
        )

        assertEquals(
            Type(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("User"),
                extends = emptyList(),
                shape = Type.Shape(
                    listOf(
                        Field(
                            identifier = FieldIdentifier("id"),
                            annotations = emptyList(),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P32, null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("username"),
                            annotations = emptyList(),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.String(null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("passwordHash"),
                            annotations = emptyList(),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.String(null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("signupDate"),
                            annotations = emptyList(),
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(Reference.Primitive.Type.Precision.P64, null),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("emailAddresses"),
                            annotations = emptyList(),
                            reference = Reference.Iterable(
                                reference = Reference.Custom(
                                    "EmailAddress",
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("twitterAccounts"),
                            annotations = emptyList(),
                            reference = Reference.Iterable(
                                reference = Reference.Custom(
                                    "TwitterAccount",
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                        Field(
                            identifier = FieldIdentifier("toDoItems"),
                            annotations = emptyList(),
                            reference = Reference.Iterable(
                                reference = Reference.Custom(
                                    "ToDoItem",
                                    isNullable = false,
                                ),
                                isNullable = false,
                            ),
                        ),
                    ),
                ),
            ),
            ast.modules.first().statements.first(),
        )

        assertEquals(
            Channel(
                comment = null,
                annotations = emptyList(),
                identifier = DefinitionIdentifier("User"),
                reference = Reference.Custom(
                    "User",
                    isNullable = false,
                ),
            ),
            ast.modules.first().statements.last(),
        )
    }
}
