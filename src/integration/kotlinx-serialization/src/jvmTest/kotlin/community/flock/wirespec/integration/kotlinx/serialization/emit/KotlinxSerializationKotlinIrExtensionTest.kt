package community.flock.wirespec.integration.kotlinx.serialization.emit

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
import community.flock.wirespec.ir.transformer.applyExtensions
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class KotlinxSerializationKotlinIrExtensionTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private fun serializableEmitter(packageName: PackageName) = KotlinIrEmitter(packageName, EmitShared(false))
        .applyExtensions(listOf(KotlinxSerializationSupportExtension()))

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

    private fun emit() = serializableEmitter(PackageName("community.flock.wirespec.generated"))
        .emit(parse(source), noLogger)

    @Test
    fun `Should annotate record models with Serializable and SerialName`() {
        val todo = emit().single { it.file.endsWith("Todo.kt") }.result

        assertContains(todo, "@kotlinx.serialization.Serializable")
        assertContains(todo, """@kotlinx.serialization.SerialName("Todo")""")
        // The annotations precede the declaration they belong to.
        assertContains(
            todo,
            """@kotlinx.serialization.SerialName("Todo")
            |data class Todo(
            """.trimMargin(),
        )
    }

    @Test
    fun `Should annotate refined models with Serializable and SerialName`() {
        val todoId = emit().single { it.file.endsWith("TodoId.kt") }.result

        assertContains(todoId, "@kotlinx.serialization.Serializable")
        assertContains(
            todoId,
            """@kotlinx.serialization.SerialName("TodoId")
            |data class TodoId(
            """.trimMargin(),
        )
    }

    @Test
    fun `Should annotate unions with Serializable but no SerialName`() {
        val union = emit().single { it.file.endsWith("UserAccount.kt") }.result

        assertContains(
            union,
            """@kotlinx.serialization.Serializable
            |sealed interface UserAccount
            """.trimMargin(),
        )
        assertFalse("@kotlinx.serialization.SerialName" in union, "Unions should not get a @SerialName annotation")
    }

    @Test
    fun `Should annotate union member types as regular records`() {
        val member = emit().single { it.file.endsWith("UserAccountPassword.kt") }.result

        assertContains(
            member,
            """@kotlinx.serialization.SerialName("UserAccountPassword")
            |data class UserAccountPassword(
            """.trimMargin(),
        )
    }

    @Test
    fun `Should annotate enums with Serializable but no SerialName`() {
        val color = emit().single { it.file.endsWith("Color.kt") }.result

        assertContains(
            color,
            """@kotlinx.serialization.Serializable
            |enum class Color
            """.trimMargin(),
        )
        assertFalse("@kotlinx.serialization.SerialName" in color, "Enums should not get a @SerialName annotation")
    }

    @Test
    fun `Should not annotate the generated generator objects`() {
        val generator = emit().single { it.file.endsWith("TodoGenerator.kt") }.result

        assertFalse(
            "@kotlinx.serialization" in generator,
            "Generator helper objects are not models and must not be annotated",
        )
    }
}
