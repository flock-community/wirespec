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
import kotlin.test.fail

class TestLib {

    @Test
    fun testProduceConsume() {
        val path = Path("src/jsTest/resources/person.ws")
        val source = SystemFileSystem.source(path).buffered().readString()
        val ast = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).fold({ fail(it.toString()) }, { it })
        assertEquals(ast.modules.head.statements, ast.produce().consume().modules.head.statements)
    }

    @Test
    fun testProduceConsumeKeepsAnnotationsAndDefaults() {
        val source = """
            |@Description("A task")
            |type Todo {
            |  @Description("What needs doing") @Tag(["a", "b"]) @Config({ level: "high" })
            |  title: String = "nothing",
            |  done: Boolean = false,
            |  count: Integer = 3,
            |  ratio: Number = 0.5,
            |  status: Status = OPEN,
            |  note: String? = null
            |}
            |
            |enum Status { OPEN, CLOSED }
            |
            |@Internal
            |endpoint GetTodo GET /todos -> {
            |  @Description("Found") 200 -> Todo
            |}
        """.trimMargin()
        val ast = object : ParseContext, NoLogger {
            override val spec = WirespecSpec
        }.parse(nonEmptyListOf(ModuleContent(FileUri(""), source))).fold({ fail(it.toString()) }, { it })
        assertEquals(ast.modules.head.statements, ast.produce().consume().modules.head.statements)
    }
}
