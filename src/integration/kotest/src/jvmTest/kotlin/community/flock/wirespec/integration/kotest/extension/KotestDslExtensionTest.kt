package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.extension.applyExtensions
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class KotestDslExtensionTest {

    private val pkg = PackageName("com.example.api")

    // The test harness joins the emitted files into one String (Wirespec runtime
    // files filtered out), so assertions look for the DSL declarations in the
    // concatenated output rather than per-file.
    private fun emitter(): Emitter = KotlinIrEmitter(pkg, EmitShared(false)).applyExtensions(listOf(KotestDslExtension(pkg)))

    @Test
    fun emitsPerEndpointDslAndCatalog() {
        // `endpoint GetTodos GET /todos -> { 200 -> TodoDto[] }`
        val output = CompileMinimalEndpointTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public class GetTodosCall internal constructor()"
        output shouldContain "endpointCall(GetTodos.Handler, GetTodos)"
        output shouldContain "inline fun <reified R : GetTodos.Response<*>> expecting(): R"
        // One catalog object per source module exposes the endpoint as a `val`.
        output shouldContain "public val getTodos: GetTodosCall"
    }

    @Test
    fun emitsPerChannelDsl() {
        // `channel Queue -> String`
        val output = CompileChannelTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public class QueueCall internal constructor()"
        output shouldContain "channelCall<String>(Queue::class)"
    }
}
