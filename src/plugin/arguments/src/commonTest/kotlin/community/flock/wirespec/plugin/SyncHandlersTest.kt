package community.flock.wirespec.plugin

import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.ir.extension.SyncHandlersExtension
import community.flock.wirespec.compiler.utils.noLogger
import community.flock.wirespec.plugin.io.Name
import community.flock.wirespec.plugin.io.Source
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class SyncHandlersTest {

    private val source =
        // language=ws
        """
        |endpoint GetTodos GET /todos -> {
        |  200 -> String
        |}
        |rpc Ping {} -> String
        """.trimMargin()

    private fun compileTo(language: Language, syncHandlers: Boolean): String {
        var output = ""
        val packageName = PackageName("community.flock.wirespec.generated")
        compile(
            CompilerArguments(
                input = nonEmptySetOf(Source(Name("todo"), source)),
                emitter = language.toEmitter(packageName, EmitShared(false))
                    .let { it.withExtensions(listOfNotNull(SyncHandlersExtension(it.extension).takeIf { syncHandlers })) },
                writer = { emitted -> output = emitted.joinToString("\n") { it.result } },
                error = { output = it },
                packageName = packageName,
                logger = noLogger,
                shared = false,
                strict = false,
            ),
        )
        return output
    }

    @Test
    fun testTypeScriptHandlersReturnPlainValues() {
        compileTo(Language.TypeScript, syncHandlers = false).run {
            shouldContain("getTodos(_request: Request): Promise<Response<unknown>>")
            shouldContain("ping(): Promise<string>")
        }
        compileTo(Language.TypeScript, syncHandlers = true).run {
            shouldContain("getTodos(_request: Request): Response<unknown>")
            shouldContain("ping(): string")
            // The client keeps calling through the asynchronous transportation.
            shouldContain("getTodos(): Promise<Response<unknown>>")
        }
    }

    @Test
    fun testPythonHandlersAreNoCoroutines() {
        compileTo(Language.Python, syncHandlers = false).run {
            shouldContain("async def get_todos(self, request: Request)")
            shouldContain("async def Ping(self)")
        }
        compileTo(Language.Python, syncHandlers = true).run {
            shouldNotContain("async def get_todos(self, request: Request)")
            shouldContain("def get_todos(self, request: Request)")
            shouldNotContain("async def Ping(self)")
            shouldContain("async def get_todos(self)")
        }
    }

    @Test
    fun testRustAndScalaAreLeftAsTheyAre() {
        listOf(Language.Rust, Language.Scala).forEach { language ->
            compileTo(language, syncHandlers = true) shouldBe compileTo(language, syncHandlers = false)
        }
    }
}
