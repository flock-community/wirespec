package community.flock.wirespec.openapi.convert

import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.IO
import io.kotest.assertions.arrow.core.shouldBeRight
import org.junit.Test
import community.flock.wirespec.openapi.v2.OpenApiV2Parser.Companion as OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser.Companion as OpenApiV3Parser

class ConvertAndCompile {

    @Test
    fun testV2ConversionAndCompilation() {
        val input = IO.readFile("v2/petstore.json")
        val ast = OpenApiV2Parser.parse(input, true)
        val wirespec = WirespecEmitter().emit(ast).joinToString("\n") { it.result }
        WirespecSpec.compile(wirespec)(noLogger)(KotlinEmitter()).shouldBeRight()
    }

    @Test
    fun testV3ConversionAndCompilation() {
        val input = IO.readFile("v3/petstore.json")
        val ast = OpenApiV3Parser.parse(input, true)
        val wirespec = WirespecEmitter().emit(ast).joinToString("\n") { it.result }
        WirespecSpec.compile(wirespec)(noLogger)(KotlinEmitter()).shouldBeRight()
    }
}
