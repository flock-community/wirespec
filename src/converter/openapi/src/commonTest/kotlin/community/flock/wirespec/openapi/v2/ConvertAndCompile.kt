package community.flock.wirespec.openapi.v2

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.common.compile
import community.flock.wirespec.openapi.v2.OpenAPIV2Parser
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class ConvertAndCompile {

    @Test
    fun testV2ConversionAndCompilation() {
        val input = Path("src/commonTest/resources/v2/petstore.json")
            .let(SystemFileSystem::source)
            .buffered()
            .readString()

        val ast = OpenAPIV2Parser.parse(ModuleContent(FileUri("SomeTestFileUri"), input), true)

        WirespecEmitter()
            .emit(ast, noLogger)
            .joinToString("\n") { it.result }
            .let(::compile)
            .shouldBeRight()
    }
}
