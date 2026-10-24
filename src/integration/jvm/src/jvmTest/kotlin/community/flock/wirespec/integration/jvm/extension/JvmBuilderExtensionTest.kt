package community.flock.wirespec.integration.jvm.extension

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitted
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.RawElement
import community.flock.wirespec.compiler.core.ir.Struct
import community.flock.wirespec.compiler.core.ir.emit.IrEmitter
import community.flock.wirespec.compiler.core.ir.extension.IrExtension
import community.flock.wirespec.compiler.core.ir.extension.applyExtensions
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.java.JavaEmitter
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import community.flock.wirespec.compiler.core.ir.File as LanguageFile
import community.flock.wirespec.integration.jvm.java.generated.model.Owner as JavaOwner
import community.flock.wirespec.integration.jvm.java.generated.model.Todo as JavaTodo
import community.flock.wirespec.integration.jvm.java.generated.model.TodoId as JavaTodoId
import community.flock.wirespec.integration.jvm.kotlin.generated.model.Owner as KotlinOwner
import community.flock.wirespec.integration.jvm.kotlin.generated.model.Todo as KotlinTodo
import community.flock.wirespec.integration.jvm.kotlin.generated.model.TodoId as KotlinTodoId

class JvmBuilderExtensionTest {

    @Test
    fun `Java builder builds a record`() {
        val todo = JavaTodo.builder()
            .withId(JavaTodoId("1"))
            .withName("Groceries")
            .withFinal(false)
            .withOwner(JavaOwner("Ann"))
            .withTags(listOf("home"))
            .withHouseNumber(42L)
            .build()

        assertEquals(JavaTodo(JavaTodoId("1"), "Groceries", false, JavaOwner("Ann"), listOf("home"), 42L, Optional.empty()), todo)
        assertEquals(todo.copyWithName("Laundry"), todo.toBuilder().withName("Laundry").build())
    }

    @Test
    fun `Java builder fails on a missing required field`() {
        val error = assertFailsWith<IllegalStateException> { JavaTodo.builder().withId(JavaTodoId("1")).build() }
        assertEquals("Todo.name is required", error.message)
    }

    @Test
    fun `Kotlin builder builds a data class`() {
        val todo = KotlinTodo.builder()
            .withId(KotlinTodoId("1"))
            .withName("Groceries")
            .withFinal(false)
            .withOwner(KotlinOwner("Ann"))
            .withTags(listOf("home"))
            .withHouseNumber(42L)
            .withDescription("Milk")
            .build()

        assertEquals(KotlinTodo(KotlinTodoId("1"), "Groceries", false, KotlinOwner("Ann"), listOf("home"), 42L, "Milk"), todo)
        assertEquals(todo.copy(name = "Laundry"), todo.toBuilder().withName("Laundry").build())
    }

    @Test
    fun `Kotlin builder fails on a missing required field`() {
        val error = assertFailsWith<IllegalStateException> { KotlinTodo.builder().withId(KotlinTodoId("1")).build() }
        assertEquals("Todo.name is required", error.message)
    }

    @Test
    fun `Scala builder builds a case class`() {
        scalaCheck("buildsCaseClass")
    }

    @Test
    fun `Scala builder handles enum and empty type fields`() {
        scalaCheck("buildsWithEnumAndEmptyType")
    }

    @Test
    fun `Scala builder fails on a missing required field`() {
        assertEquals("Todo.name is required", scalaCheck("missingRequiredFieldMessage"))
    }

    @Test
    fun `Kotlin builder replaces an empty companion added by an earlier extension`() {
        val emptyCompanion = IrExtension { ir, _ ->
            ir.map {
                (it as? LanguageFile)?.let { file ->
                    file.copy(
                        elements = file.elements.map { element ->
                            (element as? Struct)
                                ?.let { struct -> struct.copy(elements = struct.elements + RawElement("companion object")) }
                                ?: element
                        },
                    )
                } ?: it
            }
        }
        val todo = KotlinEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false))
            .applyExtensions(nonEmptyListOf(emptyCompanion, JvmBuilderExtension(FileExtension.Kotlin)))
            .emit(parse(SOURCE), noLogger)
            .file("model/Todo.kt")

        assertEquals(1, Regex("companion object").findAll(todo).count())
        assertContains(todo, "fun builder(): Builder = Builder()")
    }

    @Test
    fun `Records without fields and endpoint structs get no builder`() {
        val emitted = emit(JavaEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false)), FileExtension.Java)

        assertFalse("Builder" in emitted.file("model/Empty.java"))
        assertFalse("Builder" in emitted.file("endpoint/GetTodo.java"))
    }

    @Test
    fun `Other languages are left untouched`() {
        val emitter = JavaEmitter(PackageName("community.flock.wirespec.generated"), EmitShared(false))
        assertEquals(emitter.emit(parse(SOURCE), noLogger).toList(), emit(emitter, FileExtension.TypeScript))
    }

    // Scala compiles after the Kotlin tests, so the Scala side is reached reflectively.
    private fun scalaCheck(name: String): Any? = Class.forName("community.flock.wirespec.integration.jvm.extension.ScalaBuilderCheck").getMethod(name).invoke(null)

    private fun JavaTodo.copyWithName(name: String) = JavaTodo(id, name, _final, owner, tags, houseNumber, description)

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).getOrNull() ?: error("Parsing failed.")

    private fun emit(emitter: IrEmitter, language: FileExtension): List<Emitted> = emitter
        .applyExtensions(nonEmptyListOf(JvmBuilderExtension(language)))
        .emit(parse(SOURCE), noLogger)
        .toList()

    private fun List<Emitted>.file(suffix: String): String = single { it.file.endsWith(suffix) }.result

    private companion object {
        val SOURCE =
            """
            |type Todo {
            |  name: String,
            |  done: Boolean
            |}
            |type Empty {}
            |endpoint GetTodo GET /todos/{id: String} -> {
            |  200 -> Todo
            |}
            """.trimMargin()
    }
}
