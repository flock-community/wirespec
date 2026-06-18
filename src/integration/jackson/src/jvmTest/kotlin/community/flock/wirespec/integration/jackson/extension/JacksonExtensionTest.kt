package community.flock.wirespec.integration.jackson.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.extension.applyExtensions
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class JacksonExtensionTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private fun jacksonEmitter(packageName: PackageName) = KotlinIrEmitter(packageName, EmitShared(false))
        .applyExtensions(listOf(JacksonExtension()))

    private val source =
        """
        |type Todo {
        |  id: TodoId,
        |  name: String
        |}
        |type TodoId = String(/^[0-9]+${'$'}/g)
        |type UserAccount = UserAccountPassword | UserAccountToken
        |type UserAccountPassword {
        |  username: String,
        |  password: String
        |}
        |type UserAccountToken {
        |  token: String
        |}
        |enum Color { Red, Green, Blue }
        """.trimMargin()

    private fun emit() = jacksonEmitter(PackageName("community.flock.wirespec.generated"))
        .emit(parse(source), noLogger)

    @Test
    fun `Should annotate record fields with JsonProperty`() {
        val todo = emit().single { it.file.endsWith("Todo.kt") }.result

        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("id") val id: TodoId""")
        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("name") val name: String""")
    }

    @Test
    fun `Should annotate unions with JsonTypeInfo and JsonSubTypes`() {
        val union = emit().single { it.file.endsWith("UserAccount.kt") }.result

        assertContains(union, "@com.fasterxml.jackson.annotation.JsonTypeInfo(")
        assertContains(union, """property = "type")""")
        assertContains(union, "@com.fasterxml.jackson.annotation.JsonSubTypes(")
        assertContains(
            union,
            """com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = UserAccountPassword::class, name = "UserAccountPassword")""",
        )
        assertContains(
            union,
            """com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = UserAccountToken::class, name = "UserAccountToken")""",
        )
        // The annotations precede the declaration they belong to.
        assertContains(union, "sealed interface UserAccount")
    }

    @Test
    fun `Should annotate union member records with JsonProperty`() {
        val member = emit().single { it.file.endsWith("UserAccountPassword.kt") }.result

        assertContains(member, """@com.fasterxml.jackson.annotation.JsonProperty("username") val username: String""")
        assertContains(member, """@com.fasterxml.jackson.annotation.JsonProperty("password") val password: String""")
    }

    @Test
    fun `Should not annotate enums`() {
        val color = emit().single { it.file.endsWith("Color.kt") }.result

        assertFalse(
            "com.fasterxml.jackson.annotation" in color,
            "Enums should not receive Jackson annotations",
        )
    }

    @Test
    fun `Should not annotate the generated generator objects`() {
        val generator = emit().single { it.file.endsWith("TodoGenerator.kt") }.result

        assertFalse(
            "com.fasterxml.jackson.annotation" in generator,
            "Generator helper objects are not models and must not be annotated",
        )
    }
}
