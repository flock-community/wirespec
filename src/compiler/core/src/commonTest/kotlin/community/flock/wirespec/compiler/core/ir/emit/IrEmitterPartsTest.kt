package community.flock.wirespec.compiler.core.ir.emit

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.exceptions.NothingToEmitException
import community.flock.wirespec.compiler.core.ir.Element
import community.flock.wirespec.compiler.core.ir.File
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.generator.Generator
import community.flock.wirespec.compiler.core.parse.ast.Channel
import community.flock.wirespec.compiler.core.parse.ast.Endpoint
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Rpc
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import community.flock.wirespec.compiler.core.parse.ast.Enum as AstEnum

class IrEmitterPartsTest {

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
        override fun emit(rpc: Rpc): File = TODO()
    }

    private fun compile(vararg sources: String) = object : CompilationContext, NoLogger {
        override val emitters = nonEmptySetOf<Emitter>(TestEmitter)
    }.compile(
        nonEmptyListOf(ModuleContent(FileUri("test0.ws"), sources.first())) +
            sources.drop(1).mapIndexed { index, source -> ModuleContent(FileUri("test${index + 1}.ws"), source) },
    )

    @Test
    fun partsAreLeftOutOfTheEmittedFiles() {
        compile(
            "part Audited { createdAt: String }",
            "type Todo { ...Audited, task: String }",
        )
            .shouldBeRight()
            .map { it.file } shouldBe listOf("Todo.kt")
    }

    @Test
    fun aSpecWithOnlyPartsHasNothingToEmit() {
        compile("part Audited { createdAt: String }")
            .shouldBeLeft()
            .single()
            .shouldBeInstanceOf<NothingToEmitException>()
            .message shouldBe "Nothing to emit for Kotlin: the spec only contains parts, " +
            "and a part is only generated through the types, endpoints and rpcs that spread it."
    }
}
