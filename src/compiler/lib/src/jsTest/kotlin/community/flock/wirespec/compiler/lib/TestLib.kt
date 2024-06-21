package community.flock.wirespec.compiler.lib

import arrow.core.Either
import arrow.core.NonEmptyList
import com.goncalossilva.resources.Resource
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.compile
import community.flock.wirespec.compiler.core.emit.common.Emitted
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.core.exceptions.WirespecException
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.noLogger
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLib {

    @Test
    fun testProduceConsume(){
        val source = Resource("src/jsTest/resources/person.ws").readText()
        val res = WirespecSpec.parse(source)(noLogger)
        res.map { ast ->
            val output = ast.produce()
            val input = output.map { it.consume() }
            assertEquals(input, ast)
        }
    }

}