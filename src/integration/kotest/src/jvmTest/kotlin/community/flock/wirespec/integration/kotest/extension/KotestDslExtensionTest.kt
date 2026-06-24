package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
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
    fun emitsPerEndpointDslWithCallExtension() {
        // `endpoint GetTodos GET /todos -> { 200 -> TodoDto[] }`
        val output = CompileMinimalEndpointTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public class GetTodosScope internal constructor()"
        output shouldContain "endpointCall(GetTodos.Handler, GetTodos)"
        output shouldContain "inline fun <reified R : GetTodos.Response<*>> expecting(): R"
        // The entry point is a `call` extension on the generated endpoint object.
        output shouldContain "public suspend fun <R> GetTodos.call(block: suspend GetTodosScope.() -> R): R"
    }

    @Test
    fun blockStyleSlotsAreVarsValidatedOnFlush() {
        // `PutTodo PUT … /todos/{id: String} ?{done: Boolean, name: String?} #{token: Token, …}`
        // has a required path (id), query (done) and header (token) slot — each non-nullable.
        val output = CompileFullEndpointTest.compiler(::emitter).shouldBeRight()

        // The scope exposes each slot as an assignable builder-lambda `var`.
        output shouldContain "public class PutTodoScope internal constructor()"
        output shouldContain "public var path: (PutTodoPathBuilder.() -> Unit)? = null"
        output shouldContain "public var query: (PutTodoQueryBuilder.() -> Unit)? = null"
        output shouldContain "public var header: (PutTodoHeaderBuilder.() -> Unit)? = null"
        output shouldContain "public var body: (PutTodoPotentialTodoDtoBodyBuilder.() -> Unit)? = null"

        // Slot builders carry one `var` per field; nullable/invalid names are backtick-escaped.
        output shouldContain "public class PutTodoPathBuilder {"
        output shouldContain "public var id: Gen<String>? = null"
        output shouldContain "public var done: Gen<Boolean>? = null"
        output shouldContain "public var name: Gen<String?>? = null"
        output shouldContain "public var `Refresh-Token`: Gen<Token?>? = null"

        // flush() validates required slots/fields and defaults nullable ones; the wire name
        // stays raw while the Kotlin reference is escaped.
        output shouldContain "PutTodoPathBuilder().apply(path ?: error(\"PutTodo: required `path` block is missing\"))"
        output shouldContain "inner.pathGen(\"id\", pathBuilder.id ?: error(\"PutTodo.path: required `id` is missing\"))"
        output shouldContain "inner.queryGen(\"name\", queryBuilder.name ?: Arb.constant(null))"
        output shouldContain "inner.headerGen(\"Refresh-Token\", headerBuilder.`Refresh-Token` ?: Arb.constant(null))"

        // Terminals flush before executing; the `call` extension opens the scope as a block.
        output shouldContain "public suspend inline fun <reified R : PutTodo.Response<*>> expecting(): R = expectingClass(R::class)"
        output shouldContain "public suspend fun <R> PutTodo.call(block: suspend PutTodoScope.() -> R): R"
    }

    @Test
    fun emitsPerChannelDsl() {
        // `channel Queue -> String`
        val output = CompileChannelTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public class QueueCall internal constructor()"
        output shouldContain "channelCall<String>(Queue::class)"
        // The entry point is a `call` extension on the generated channel object.
        output shouldContain "public suspend fun <R> Queue.call(block: suspend QueueCall.() -> R): R"
    }
}
