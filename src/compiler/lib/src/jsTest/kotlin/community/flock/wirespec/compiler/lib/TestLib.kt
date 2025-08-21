package community.flock.wirespec.compiler.lib

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
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
        object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).map { ast ->
            val output = ast.produce()
            val input = output.consume()
            assertEquals(input, ast)
        }
    }
}
