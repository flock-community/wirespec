package community.flock.wirespec.compiler.core.ir.extension

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.FileExtension
import community.flock.wirespec.compiler.core.ir.Function
import community.flock.wirespec.compiler.core.ir.IR
import community.flock.wirespec.compiler.core.ir.Interface
import community.flock.wirespec.compiler.core.ir.Name
import community.flock.wirespec.compiler.core.ir.Type
import community.flock.wirespec.compiler.core.ir.converter.convert
import community.flock.wirespec.compiler.core.ir.file
import community.flock.wirespec.compiler.core.ir.findAll
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.fail

class SyncExtensionTest {

    private val ast = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(
        nonEmptyListOf(
            ModuleContent(
                FileUri("todo.ws"),
                """
                |endpoint GetTodos GET /todos -> { 200 -> String }
                |channel Queue -> String
                |rpc Ping {} -> String
                """.trimMargin(),
            ),
        ),
    ).getOrElse { fail("Parse failed: $it") }

    private val asyncChannel = file("AsyncQueue") {
        namespace("AsyncQueue", type("Wirespec.Channel")) {
            `interface`("Sender") {
                asyncFunction("asyncQueue") { arg("message", string) }
            }
            `interface`("Listener") {
                asyncFunction("asyncQueue") { arg("handler", Type.Function(listOf(string), unit, isAsync = true)) }
            }
        }
    }

    private fun AST.ir(): IR = modules.flatMap { module -> module.statements.map { it.convert() } } + asyncChannel

    private fun IR.asyncOf(vararg interfaces: String): List<Boolean> = toList().flatMap { it.findAll<Interface>() }
        .filter { iface -> interfaces.any { iface.name == Name.of(it) } }
        .flatMap { it.findAll<Function>() }
        .flatMap { function -> listOf(function.isAsync) + function.parameters.mapNotNull { (it.type as? Type.Function)?.isAsync } }

    @Test
    fun makesHandlersChannelsAndServicesSynchronous() {
        val ir = SyncExtension(FileExtension.Kotlin).extend(ast.ir(), ast)

        ir.asyncOf("Handler", "Sender", "Listener", "Service").toSet() shouldBe setOf(false)
    }

    @Test
    fun keepsTheClientSideAsynchronous() {
        val ir = SyncExtension(FileExtension.Kotlin).extend(ast.ir(), ast)

        ir.asyncOf("Call").toSet() shouldBe setOf(true)
    }

    @Test
    fun leavesRustAndScalaAsTheyAre() {
        listOf(FileExtension.Rust, FileExtension.Scala).forEach { language ->
            SyncExtension(language).extend(ast.ir(), ast) shouldBe ast.ir()
        }
    }
}
