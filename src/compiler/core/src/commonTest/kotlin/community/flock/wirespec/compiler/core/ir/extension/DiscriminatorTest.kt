package community.flock.wirespec.compiler.core.ir.extension

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class DiscriminatorTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source.trimMargin())))
        .getOrElse { fail("Parse failed: $it") }

    private fun errors(source: String) = parse(source).discriminatorErrors("TestDiscriminator").map { it.fileUri.value to it.message }

    @Test
    fun shouldReadTheDiscriminatorOfAnAnnotatedUnion() {
        val ast = parse(
            """
            |@TestDiscriminator("kind")
            |type Pet = Dog | Cat
            |type Dog { kind: "dog", name: String }
            |type Cat { kind: "cat", lives: Integer }
            |type Account = Password | Token
            |type Password { password: String }
            |type Token { token: String }
            """,
        )

        assertEquals(
            mapOf("Pet" to Discriminator(field = "kind", literals = mapOf("Dog" to "dog", "Cat" to "cat"))),
            ast.discriminators("TestDiscriminator"),
        )
        assertEquals(emptyList(), ast.discriminatorErrors("TestDiscriminator"))
    }

    @Test
    fun shouldIgnoreTheAnnotationOfAnotherExtension() {
        val ast = parse(
            """
            |@OtherDiscriminator("kind")
            |type Pet = Dog | Cat
            |type Dog { name: String }
            |type Cat { lives: Integer }
            """,
        )

        assertEquals(emptyMap(), ast.discriminators("TestDiscriminator"))
        assertEquals(emptyList(), ast.discriminatorErrors("TestDiscriminator"))
    }

    @Test
    fun shouldReportAnAnnotationWithoutAField() {
        assertEquals(
            listOf("test.ws" to "@TestDiscriminator on union 'Pet' needs the name of a field: @TestDiscriminator(\"kind\")"),
            errors(
                """
                |@TestDiscriminator
                |type Pet = Dog | Cat
                |type Dog { kind: "dog" }
                |type Cat { kind: "cat" }
                """,
            ),
        )
    }

    @Test
    fun shouldReportEveryMemberWithoutTheLiteral() {
        assertEquals(
            listOf(
                "test.ws" to "@TestDiscriminator(\"kind\") on union 'Pet': its member 'Cat' is not a type with a field 'kind' of a string literal type",
                "test.ws" to "@TestDiscriminator(\"kind\") on union 'Pet': its member 'Bird' is not a type with a field 'kind' of a string literal type",
            ),
            errors(
                """
                |@TestDiscriminator("kind")
                |type Pet = Dog | Cat | Bird
                |type Dog { kind: "dog" }
                |type Cat { kind: String }
                |type Bird { name: String }
                """,
            ),
        )
    }

    @Test
    fun shouldReportMembersWithTheSameLiteral() {
        assertEquals(
            listOf("test.ws" to "@TestDiscriminator(\"kind\") on union 'Pet': its members Dog and Cat all have \"pet\" in it"),
            errors(
                """
                |@TestDiscriminator("kind")
                |type Pet = Dog | Cat
                |type Dog { kind: "pet" }
                |type Cat { kind: "pet" }
                """,
            ),
        )
    }
}
