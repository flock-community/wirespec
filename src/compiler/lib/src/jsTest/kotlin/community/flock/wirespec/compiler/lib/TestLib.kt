package community.flock.wirespec.compiler.lib

import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.utils.NoLogger
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

class TestLib {

    @Test
    fun testProduceConsume() {
        val path = Path("src/jsTest/resources/person.ws")
        val source = SystemFileSystem.source(path).buffered().readString()
        println(source)
        object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(source).map { ast ->
            val output = ast.produce()
            val input = output.map { it.consume() }
            assertEquals(input, ast)
        }
    }
}
