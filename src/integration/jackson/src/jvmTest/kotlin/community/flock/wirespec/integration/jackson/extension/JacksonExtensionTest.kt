package community.flock.wirespec.integration.jackson.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
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
        |type TodoId = String(/^[0-9]+$/g)
        |type UserAccount = UserAccountPassword | UserAccountToken
        |type UserAccountPassword {
        |  username: String,
        |  password: String
        |}
        |type UserAccountToken {
        |  token: String
        |}
        |@JacksonDiscriminator("kind")
        |type Pet = Dog | Cat
        |type Dog {
        |  kind: "dog",
        |  name: String
        |}
        |type Cat {
        |  kind: "cat",
        |  lives: Integer
        |}
        |enum Color { Red, Green, Blue }
        """.trimMargin()

    private fun emit(emitter: IrEmitter) = emitter
        .applyExtensions(nonEmptyListOf(JacksonExtension(emitter.extension)))
        .emit(parse(source), noLogger)

    private fun kotlin() = emit(KotlinEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false)))
    private fun java() = emit(JavaEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false)))

    private fun List<community.flock.wirespec.compiler.core.emit.Emitted>.file(suffix: String) = single { it.file.endsWith(suffix) }.result

    // ----- Kotlin -----

    @Test
    fun `Kotlin record fields get JsonProperty`() {
        val todo = kotlin().file("Todo.kt")
        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("id") val id: TodoId""")
        assertContains(todo, """@com.fasterxml.jackson.annotation.JsonProperty("name") val name: String""")
    }

    @Test
    fun `Kotlin unions with a discriminator get JsonTypeInfo and JsonSubTypes on that field`() {
        assertContains(
            kotlin().file("Pet.kt"),
            """
            |@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, include = com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
            |@com.fasterxml.jackson.annotation.JsonSubTypes(com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Dog::class, name = "dog"), com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Cat::class, name = "cat"))
            |public sealed interface Pet
            """.trimMargin(),
        )
    }

    @Test
    fun `Kotlin unions without a discriminator are not annotated`() {
        assertFalse("com.fasterxml.jackson.annotation" in kotlin().file("UserAccount.kt"))
    }

    @Test
    fun `Kotlin union members get JsonProperty and no type name`() {
        val member = kotlin().file("UserAccountPassword.kt")
        assertFalse("JsonTypeName" in member)
        assertFalse("JsonTypeName" in kotlin().file("Dog.kt"))
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
    fun `Java unions with a discriminator get JsonTypeInfo and JsonSubTypes on that field`() {
        assertContains(
            java().file("Pet.java"),
            """
            |@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, include = com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY, property = "kind", visible = true)
            |@com.fasterxml.jackson.annotation.JsonSubTypes({@com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Dog.class, name = "dog"), @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Cat.class, name = "cat")})
            |public sealed interface Pet
            """.trimMargin(),
        )
    }

    @Test
    fun `Java unions without a discriminator are not annotated`() {
        assertFalse("com.fasterxml.jackson.annotation" in java().file("UserAccount.java"))
    }

    @Test
    fun `Java union members get JsonProperty and no type name`() {
        val member = java().file("UserAccountPassword.java")
        assertFalse("JsonTypeName" in member)
        assertFalse("JsonTypeName" in java().file("Dog.java"))
        assertContains(member, """@com.fasterxml.jackson.annotation.JsonProperty("username") String username""")
    }

    @Test
    fun `Java enums are not annotated`() {
        assertFalse("com.fasterxml.jackson.annotation" in java().file("Color.java"))
    }

    @Test
    fun `A discriminator that is not a literal in every member is reported`() {
        val ast = parse(
            """
            |@JacksonDiscriminator("kind")
            |type Pet = Dog | Cat
            |type Dog { kind: "dog" }
            |type Cat { kind: String }
            """.trimMargin(),
        )

        assertEquals(
            listOf("@JacksonDiscriminator(\"kind\") on union 'Pet': its member 'Cat' is not a type with a field 'kind' of a string literal type"),
            JacksonExtension(FileExtension.Kotlin).validate(ast).map { it.message },
        )
    }
}
