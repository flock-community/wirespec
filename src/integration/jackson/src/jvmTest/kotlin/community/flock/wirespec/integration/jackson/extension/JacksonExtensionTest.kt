package community.flock.wirespec.integration.jackson.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaIrEmitter
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.extension.applyExtensions
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class JacksonExtensionTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

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

    private fun emit(emitter: Emitter) = emitter
        .applyExtensions(listOf(JacksonExtension()))
        .emit(parse(source), noLogger)

    private fun kotlin() = emit(KotlinIrEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false)))
    private fun java() = emit(JavaIrEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false)))

    private fun List<community.flock.wirespec.compiler.core.emit.Emitted>.file(suffix: String) = single { it.file.endsWith(suffix) }.result

    // ----- Kotlin -----

    @Test
    fun `Kotlin record fields get JsonProperty`() {
        val todo = kotlin().file("Todo.kt")
        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("id") val id: TodoId""")
        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("name") val name: String""")
    }

    @Test
    fun `Kotlin unions get JsonTypeInfo`() {
        val union = kotlin().file("UserAccount.kt")
        assertContains(union, "@com.fasterxml.jackson.annotation.JsonTypeInfo(")
        assertContains(union, """property = "type")""")
        assertContains(union, "sealed interface UserAccount")
    }

    @Test
    fun `Kotlin union members get JsonTypeName and JsonProperty`() {
        val member = kotlin().file("UserAccountPassword.kt")
        assertContains(member, """@com.fasterxml.jackson.annotation.JsonTypeName("UserAccountPassword")""")
        assertContains(member, """@com.fasterxml.jackson.annotation.JsonProperty("username") val username: String""")
    }

    @Test
    fun `Kotlin enums are not annotated`() {
        assertFalse("com.fasterxml.jackson.annotation" in kotlin().file("Color.kt"))
    }

    // ----- Java -----

    @Test
    fun `Java record fields get JsonProperty`() {
        val todo = java().file("Todo.java")
        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("id") TodoId id""")
        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("name") String name""")
    }

    @Test
    fun `Java unions get JsonTypeInfo`() {
        val union = java().file("UserAccount.java")
        assertContains(union, "@com.fasterxml.jackson.annotation.JsonTypeInfo(")
        assertContains(union, """property = "type")""")
        assertContains(union, "sealed interface UserAccount")
    }

    @Test
    fun `Java union members get JsonTypeName and JsonProperty`() {
        val member = java().file("UserAccountPassword.java")
        assertContains(member, """@com.fasterxml.jackson.annotation.JsonTypeName("UserAccountPassword")""")
        assertContains(member, """@com.fasterxml.jackson.annotation.JsonProperty("username") String username""")
    }

    @Test
    fun `Java enums are not annotated`() {
        assertFalse("com.fasterxml.jackson.annotation" in java().file("Color.java"))
    }
}
