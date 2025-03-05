package community.flock.wirespec.openapi.convert

import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class ConvertAndCompile {

    @Test
    fun testV2ConversionAndCompilation() {
        val path = Path("src/commonTest/resources/v2/petstore.json")
        val input = SystemFileSystem.source(path).buffered().readString()
        val ast = OpenApiV2Parser.parse(input, true)
        val wirespec = WirespecEmitter(noLogger).emit(ast).joinToString("\n") { it.result }
        compiler(wirespec).shouldBeRight()
    }

    @Test
    fun testV3ConversionAndCompilation() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val input = SystemFileSystem.source(path).buffered().readString()
        val ast = OpenApiV3Parser.parse(input, true)
        val wirespec = WirespecEmitter(noLogger).emit(ast).joinToString("\n") { it.result }
        compiler(wirespec).shouldBeRight()
    }

    private fun compiler(source: String) = object : CompilationContext {
        override val logger = noLogger
        override val emitter = KotlinEmitter(logger = logger)
    }.compile(source)
}
