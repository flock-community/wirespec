package community.flock.wirespec.compiler.lib

import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLib {

    @Test
    fun testProduceConsume() {
        val source = Resource("src/jsTest/resources/person.ws").readText()
        println(source)
        object : ParseContext {
            override val spec = WirespecSpec
            override val logger = noLogger
        }.parse(source).map { ast ->
            val output = ast.produce()
            val input = output.filterIsInstance<WsDefinition>().map { it.consume() }
            assertEquals(input, ast)
        }
    }
}
