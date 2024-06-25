package community.flock.wirespec.openapi.convert

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.openapi.v2.OpenApiV2Parser
import community.flock.wirespec.openapi.v3.OpenApiV3Parser
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class ConvertAndCompile {

    @Test
    fun testV2ConversionAndCompilation() {
        val input = Resource("src/commonTest/resources/v2/petstore.json").readText()
        val ast = OpenApiV2Parser.parse(input, true)
        val wirespec = WirespecEmitter().emit(ast).joinToString("\n") { it.result }
        WirespecSpec.compile(wirespec)(noLogger)(KotlinEmitter()).shouldBeRight()
    }

    @Test
    fun testV3ConversionAndCompilation() {
        val input = Resource("src/commonTest/resources/v3/petstore.json").readText()
        val ast = OpenApiV3Parser.parse(input, true)
        val wirespec = WirespecEmitter().emit(ast).joinToString("\n") { it.result }
        WirespecSpec.compile(wirespec)(noLogger)(KotlinEmitter()).shouldBeRight()
    }
}
