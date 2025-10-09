package community.flock.wirespec.openapi.v3

import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.emitters.wirespec.WirespecEmitter
import community.flock.wirespec.openapi.common.compile
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test

class ConvertAndCompile {

    @Test
    fun testV3ConversionAndCompilation() {
        val input = Path("src/commonTest/resources/v3/petstore.json")
            .let(SystemFileSystem::source)
            .buffered()
            .readString()

        val ast = OpenAPIV3Parser.parse(ModuleContent(FileUri("SomeTestFileUri"), input), true)

        WirespecEmitter()
            .emit(ast, noLogger)
            .joinToString("\n") { it.result }
            .let(::compile)
            .shouldBeRight()
    }
}
