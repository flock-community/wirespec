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

        // `request` mirrors `call`'s scope block but materialises the typed request instead of sending.
        output shouldContain "public suspend fun GetTodos.request(block: suspend GetTodosScope.() -> Unit): GetTodos.Request"
        output shouldContain "public suspend fun buildRequest(): GetTodos.Request"
        output shouldContain "return inner.buildRequest()"

        // A per-variant `responseNNN { … }` builds a random response variant; the list body is a
        // whole-value `Gen<List<TodoDto>>` setter.
        output shouldContain "public class GetTodosResponse200Scope internal constructor()"
        output shouldContain "responseCall(GetTodos, GetTodos.Response200::class)"
        output shouldContain "public var body: Gen<List<TodoDto>>? = null"
        output shouldContain "public suspend fun GetTodos.response200(block: GetTodosResponse200Scope.() -> Unit = {}): GetTodos.Response200"
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

        // `request` reuses the same scope (so path/query/header/body pin identically) and returns
        // the built request.
        output shouldContain "public suspend fun PutTodo.request(block: suspend PutTodoScope.() -> Unit): PutTodo.Request"
        output shouldContain "public suspend fun buildRequest(): PutTodo.Request"

        // The 201 variant carries a `TodoDto` body plus `token`/`refreshToken` response headers, so
        // its scope exposes a whole-value body setter and one setter per header field.
        output shouldContain "public class PutTodoResponse201Scope internal constructor()"
        output shouldContain "responseCall(PutTodo, PutTodo.Response201::class)"
        output shouldContain "public var body: Gen<TodoDto>? = null"
        output shouldContain "public var token: Gen<Token>? = null"
        output shouldContain "public var refreshToken: Gen<Token?>? = null"
        output shouldContain "token?.let { inner.headerGen(\"token\", it) }"
        output shouldContain "return inner.build() as PutTodo.Response201"
        output shouldContain "public suspend fun PutTodo.response201(block: PutTodoResponse201Scope.() -> Unit = {}): PutTodo.Response201"
        // A header-less variant (500 → Error) still gets its body setter and builder.
        output shouldContain "public suspend fun PutTodo.response500(block: PutTodoResponse500Scope.() -> Unit = {}): PutTodo.Response500"
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
