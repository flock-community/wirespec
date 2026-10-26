package community.flock.wirespec.compiler.core.ir.extension

import arrow.core.getOrElse
import arrow.core.toNonEmptyListOrNull
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.generator.KotlinGenerator
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.fail

class ValidateExtensionTest {

    @Test
    fun validatesRefinedTypesDeclaredInAnotherModule() {
        val ast = parseAst(
            "type Person { email: Email, emails: { Email[] }? }",
            "type Email = String(/^.+@.+$/g)",
        )

        ValidateExtension(FileExtension.Kotlin).extend(ast.ir(), ast).validateOf("Person") shouldBe
            """
            |override fun validate(): List<String> =
            |  (if (!email.validate()) listOf("email") else emptyList<String>()) + (emails?.let { it.entries.flatMap { (k1, v1) -> v1.flatMapIndexed { i2, el2 -> if (!el2.validate()) listOf("emails[${'$'}{k1}][${'$'}{i2}]") else emptyList<String>() } } } ?: emptyList<String>())
            """.trimMargin()
    }

    @Test
    fun validatesConstrainedPrimitiveFields() {
        val ast = parseAst("type Product { code: String(/^[A-Z]+$/g), quantity: Integer(0, 10)?, name: String }")

        ValidateExtension(FileExtension.Kotlin).extend(ast.ir(), ast).validateOf("Product") shouldBe
            """
            |override fun validate(): List<String> =
            |  (if (Regex(""${'"'}^[A-Z]+$""${'"'}).matches(code)) emptyList<String>() else listOf("code")) + (quantity?.let { if (0 <= it && it <= 10) emptyList<String>() else listOf("quantity") } ?: emptyList<String>())
            """.trimMargin()
    }

    @Test
    fun validatesAModelThatAChannelIsNamedAfter() {
        val parsed = parseAst("type Order { tag: Tag }", "type Tag = String(/^[a-z]+$/g)")
        // The Avro converter declares such a channel for the record it reads.
        val channel = Channel(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Order"),
            reference = Reference.Custom("Order", isNullable = false),
        )
        val ast = parsed.copy(modules = parsed.modules.map { it.copy(statements = it.statements + channel) })

        ValidateExtension(FileExtension.Kotlin).extend(parsed.ir(), ast).validateOf("Order") shouldBe
            """
            |override fun validate(): List<String> =
            |  if (!tag.validate()) listOf("tag") else emptyList<String>()
            """.trimMargin()
    }

    @Test
    fun leavesLanguagesWithoutModelsUnchanged() {
        val ast = parseAst("type Person { name: String }")
        val ir = ast.ir()

        ValidateExtension(FileExtension.Wirespec).extend(ir, ast) shouldBe ir
    }

    private fun parseAst(vararg sources: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(sources.mapIndexed { index, source -> ModuleContent(FileUri("module$index.ws"), source) }.toNonEmptyListOrNull()!!)
        .getOrElse { fail("Parse failed: $it") }

    private fun AST.ir(): IR = modules.flatMap { module -> module.statements.map { it.convert() } }

    private fun IR.validateOf(model: String): String = filterIsInstance<File>()
        .single { it.name == Name.of(model) }
        .let(KotlinGenerator::generate)
        .lines()
        .dropWhile { !it.trim().startsWith("override fun validate()") }
        .take(2)
        .joinToString("\n") { it.drop(2) }
}
