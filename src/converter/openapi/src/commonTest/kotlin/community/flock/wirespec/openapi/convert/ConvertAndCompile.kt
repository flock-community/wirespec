package community.flock.wirespec.openapi.convert

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.CompilationContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.kotlin.KotlinEmitter
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import community.flock.wirespec.openapi.v3.OpenAPIV3Parser
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
        val ast = OpenAPIV2Parser.parse(ModuleContent(FileUri("test.ws"), input), true)
        val wirespec = WirespecEmitter().emit(ast, noLogger).joinToString("\n") { it.result }
        compiler(wirespec).shouldBeRight()
    }

    @Test
    fun testV3ConversionAndCompilation() {
        val path = Path("src/commonTest/resources/v3/petstore.json")
        val input = SystemFileSystem.source(path).buffered().readString()
        val ast = OpenAPIV3Parser.parse(ModuleContent(FileUri("test.ws"), input), true)
        val wirespec = WirespecEmitter().emit(ast, noLogger).joinToString("\n") { it.result }
        compiler(wirespec).shouldBeRight()
    }

    private fun compiler(source: String) = object : CompilationContext, NoLogger {
        override val emitters = nonEmptySetOf(KotlinEmitter())
    }.compile(nonEmptyListOf(ModuleContent(FileUri("test.ws"), source)))
}
