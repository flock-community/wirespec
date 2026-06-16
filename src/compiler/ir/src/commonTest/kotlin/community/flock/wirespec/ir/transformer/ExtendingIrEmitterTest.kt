package community.flock.wirespec.ir.transformer

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.ir.converter.convert
import community.flock.wirespec.ir.core.Element
import community.flock.wirespec.ir.core.File
import community.flock.wirespec.ir.core.Name
import community.flock.wirespec.ir.emit.IrEmitter
import community.flock.wirespec.ir.generator.Generator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.fail
import community.flock.wirespec.compiler.core.parse.ast.Enum as AstEnum

class ExtendingIrEmitterTest {

    private fun parseAst(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source)))
        .getOrElse { fail("Parse failed: $it") }

    private object TestEmitter : IrEmitter {
        override val extension = FileExtension.Kotlin
        override val generator = object : Generator {
            override fun generate(element: Element) = (element as File).name.value()
        }

        override fun emitShared(): File? = null
        override fun emit(type: Type, module: Module): File = type.convert()
        override fun emit(enum: AstEnum, module: Module): File = TODO()
        override fun emit(refined: Refined): File = TODO()
        override fun emit(endpoint: Endpoint): File = TODO()
        override fun emit(union: Union): File = TODO()
        override fun emit(channel: Channel): File = TODO()
    }

    private val ast = parseAst(
        """
        type Foo {
            bar: String
        }
        """.trimIndent(),
    )

    @Test
    fun shouldApplyExtensionsToIrBeforeGeneration() {
        var seenAst: AST? = null
        val rename = IrExtension { ir, ast ->
            seenAst = ast
            ir.map { element ->
                if (element is File) element.copy(name = Name(element.name.parts + "Transformed")) else element
            }
        }

        val emitted = ExtendingIrEmitter(TestEmitter, listOf(rename)).emit(ast, noLogger)

        assertEquals(listOf("FooTransformed.kt"), emitted.map { it.file })
        assertSame(ast, seenAst)
    }

    @Test
    fun shouldApplyExtensionsInOrder() {
        val first = IrExtension { ir, _ -> ir.map { (it as File).copy(name = Name(it.name.parts + "A")) } }
        val second = IrExtension { ir, _ -> ir.map { (it as File).copy(name = Name(it.name.parts + "B")) } }

        val emitted = ExtendingIrEmitter(TestEmitter, listOf(first, second)).emit(ast, noLogger)

        assertEquals(listOf("FooAB.kt"), emitted.map { it.file })
    }

    @Test
    fun shouldEmitUnchangedWithoutExtensions() {
        val emitted = TestEmitter.emit(ast, noLogger)

        assertEquals(listOf("Foo.kt"), emitted.map { it.file })
    }

    @Test
    fun applyExtensionsShouldOnlyWrapIrEmitters() {
        val extension = IrExtension { ir, _ -> ir }

        assertIs<ExtendingIrEmitter>(TestEmitter.applyExtensions(listOf(extension)))
        assertSame(TestEmitter, TestEmitter.applyExtensions(emptyList()))
    }
}
