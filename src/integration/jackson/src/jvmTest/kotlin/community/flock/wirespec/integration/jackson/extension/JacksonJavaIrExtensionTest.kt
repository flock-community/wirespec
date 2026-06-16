package community.flock.wirespec.integration.jackson.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.ir.extension.applyExtensions
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class JacksonJavaIrExtensionTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private fun jacksonEmitter(packageName: PackageName) = JavaIrEmitter(packageName, EmitShared(false))
        .applyExtensions(listOf(JacksonSupportExtension(FileExtension.Java)))

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
    fun `Should annotate unions with JsonTypeInfo and JsonSubTypes`() {
        val union = emit().single { it.file.endsWith("UserAccount.java") }.result

        assertContains(
            union,
            """
            |@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION)
            |@com.fasterxml.jackson.annotation.JsonSubTypes({@com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = UserAccountPassword.class), @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = UserAccountToken.class)})
            |public sealed interface UserAccount
            """.trimMargin(),
        )
    }

    @Test
    fun `Should leave records, refined types and enums untouched`() {
        val emitted = emit()

        for (fileName in listOf("Todo.java", "TodoId.java", "Color.java", "UserAccountPassword.java")) {
            val result = emitted.single { it.file.endsWith(fileName) }.result
            assertFalse(
                "com.fasterxml.jackson" in result,
                "$fileName is handled by the runtime Wirespec Jackson module and must not be annotated",
            )
        }
    }
}
