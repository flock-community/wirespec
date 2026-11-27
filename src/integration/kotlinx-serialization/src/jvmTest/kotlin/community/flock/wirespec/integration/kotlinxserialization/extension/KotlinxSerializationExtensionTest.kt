package community.flock.wirespec.integration.kotlinxserialization.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KotlinxSerializationExtensionTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private fun serializableEmitter(packageName: PackageName) = KotlinEmitter(packageName, EmitShared(false))
        .applyExtensions(nonEmptyListOf(KotlinxSerializationExtension()))

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
        |@KotlinxSerializationDiscriminator("kind")
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
            |public data class Todo(
            """.trimMargin(),
        )
    }

    @Test
    fun `Should annotate refined models with Serializable and SerialName and emit them as value classes`() {
        val todoId = emit().single { it.file.endsWith("TodoId.kt") }.result

        assertContains(todoId, "@kotlinx.serialization.Serializable")
        assertContains(
            todoId,
            """@kotlinx.serialization.SerialName("TodoId")
            |@kotlin.jvm.JvmInline
            |public value class TodoId(
            """.trimMargin(),
        )
    }

    @Test
    fun `Should read a union with a discriminator by that field`() {
        val union = emit().single { it.file.endsWith("Pet.kt") }.result

        assertContains(
            union,
            """
            |@kotlinx.serialization.Serializable(with = PetSerializer::class)
            |public sealed interface Pet
            |public object PetSerializer : kotlinx.serialization.json.JsonContentPolymorphicSerializer<Pet>(Pet::class) {
            |  override fun selectDeserializer(element: kotlinx.serialization.json.JsonElement): kotlinx.serialization.DeserializationStrategy<Pet> =
            |    when (val literal = ((element as? kotlinx.serialization.json.JsonObject)?.get("kind") as? kotlinx.serialization.json.JsonPrimitive)?.content) {
            |      "dog" -> Dog.serializer()
            |      "cat" -> Cat.serializer()
            |      else -> throw kotlinx.serialization.SerializationException("Pet has no member with the kind '${'$'}literal'")
            |    }
            |}
            """.trimMargin(),
        )
    }

    @Test
    fun `Should not read a union without a discriminator`() {
        val union = emit().single { it.file.endsWith("UserAccount.kt") }.result

        assertContains(
            union,
            """
            |@kotlinx.serialization.Serializable(with = UserAccountSerializer::class)
            |public sealed interface UserAccount
            |public object UserAccountSerializer : kotlinx.serialization.json.JsonContentPolymorphicSerializer<UserAccount>(UserAccount::class) {
            |  override fun selectDeserializer(element: kotlinx.serialization.json.JsonElement): kotlinx.serialization.DeserializationStrategy<UserAccount> =
            |    throw kotlinx.serialization.SerializationException("UserAccount cannot be read: name the field that tells its members apart with @KotlinxSerializationDiscriminator")
            |}
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
            |public data class UserAccountPassword(
            """.trimMargin(),
        )
    }

    @Test
    fun `Should annotate enums with Serializable but no SerialName`() {
        val color = emit().single { it.file.endsWith("Color.kt") }.result

        assertContains(
            color,
            """@kotlinx.serialization.Serializable
            |public enum class Color
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

    @Test
    fun `Should report a discriminator that is not a literal in every member`() {
        val ast = parse(
            """
            |@KotlinxSerializationDiscriminator("kind")
            |type Pet = Dog | Cat
            |type Dog { kind: "dog" }
            |type Cat { kind: String }
            """.trimMargin(),
        )

        assertEquals(
            listOf("@KotlinxSerializationDiscriminator(\"kind\") on union 'Pet': its member 'Cat' is not a type with a field 'kind' of a string literal type"),
            KotlinxSerializationExtension().validate(ast).map { it.message },
        )
    }
}
