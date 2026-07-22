package community.flock.wirespec.integration.kotest.extension

import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.Emitter
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.compile
import community.flock.wirespec.emitters.kotlin.KotlinIrEmitter
import community.flock.wirespec.ir.extension.applyExtensions
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.test.Test

class KotestDslExtensionTest {

    private val pkg = PackageName("com.example.api")

    // The test harness joins the emitted files into one String (Wirespec runtime
    // files filtered out), so assertions look for the DSL declarations in the
    // concatenated output rather than per-file.
    private fun emitter(): Emitter = KotlinIrEmitter(pkg, EmitShared(false)).applyExtensions(listOf(KotestDslExtension(pkg)))

    @Test
    fun emitsPerEndpointDslWithGenerateExtension() {
        // `endpoint GetTodos GET /todos -> { 200 -> TodoDto[] }`
        val output = CompileMinimalEndpointTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public class GetTodosScope internal constructor()"
        output shouldContain "endpointCall(GetTodos.Handler, GetTodos)"
        // The entry points are grouped in a `generate` extension property on the endpoint object.
        output shouldContain "public class GetTodosGenerate internal constructor()"
        output shouldContain "public val GetTodos.generate: GetTodosGenerate"

        // `request` opens the scope and returns a `Gen<Request>` (drawn/sent later).
        output shouldContain "public suspend fun request(block: suspend GetTodosScope.() -> Unit): Gen<GetTodos.Request>"
        output shouldContain "public fun buildRequest(): Gen<GetTodos.Request>"
        output shouldContain "return inner.buildRequestGen()"

        // Sending chains off the request `Gen`: `GetTodos.generate.request { … }.call()`.
        output shouldContain "public suspend fun Gen<GetTodos.Request>.call(): GetTodos.Response<*> ="
        output shouldContain "requestCall(GetTodos.Handler, GetTodos, this)"
        output shouldNotContain "call(block: suspend GetTodosScope"
        output shouldNotContain "expectingClass"

        // A per-variant `generate.responseNNN { … }` returns a `Gen<Response<NNN>>`; the list
        // body is a whole-value `Gen<List<TodoDto>>` setter.
        output shouldContain "public class GetTodosResponse200Scope internal constructor()"
        output shouldContain "responseCall(GetTodos, GetTodos.Response200::class)"
        output shouldContain "public var body: Gen<List<TodoDto>>? = null"
        output shouldContain "public fun response200(block: GetTodosResponse200Scope.() -> Unit = {}): Gen<GetTodos.Response200>"

        // Mocking chains off the response `Gen`, the response-side twin of `Gen<Request>.call()`:
        // `GetTodos.generate.response200 { … }.mock { req -> … }` stubs the drawn response.
        output shouldContain "public suspend fun Gen<GetTodos.Response<*>>.mock(predicate: (GetTodos.Request) -> Boolean): Unit ="
        output shouldContain "responseMock(GetTodos.Handler, this, predicate)"
    }

    @Test
    fun blockStyleSlotsAreVarsValidatedOnFlush() {
        // `PutTodo PUT … /todos/{id: String} ?{done: Boolean, name: String?} #{token: Token, …}`
        // has a required path (id), query (done) and header (token) slot — each non-nullable.
        val output = CompileFullEndpointTest.compiler(::emitter).shouldBeRight()

        // The scope exposes each slot only through its function form (`path { … }`); the
        // underlying builder-lambda `var` is private, so it is the sole way to set the slot.
        output shouldContain "public class PutTodoScope internal constructor()"
        output shouldContain "private var path: (PutTodoPathBuilder.() -> Unit)? = null"
        // Setter body is rendered by the IR generator, which normalises single-statement blocks
        // onto their own line rather than the inline `{ … }` form.
        output shouldContain "public fun path(block: PutTodoPathBuilder.() -> Unit) {"
        output shouldContain "this.path = block"
        output shouldContain "private var query: (PutTodoQueryBuilder.() -> Unit)? = null"
        output shouldContain "private var header: (PutTodoHeaderBuilder.() -> Unit)? = null"
        // The request body references the shared, un-prefixed `<Type>Builder` (emitted once by the
        // type DSL), not a per-endpoint `PutTodoPotentialTodoDtoBodyBuilder`.
        output shouldContain "private var body: (PotentialTodoDtoBuilder.() -> Unit)? = null"
        output shouldContain "public fun body(block: PotentialTodoDtoBuilder.() -> Unit) {"
        output shouldContain "this.body = block"
        output shouldNotContain "PutTodoPotentialTodoDtoBodyBuilder"

        // Slot builders carry one `var` per field; nullable/invalid names are backtick-escaped.
        output shouldContain "public class PutTodoPathBuilder {"
        output shouldContain "public var id: Gen<String>? = null"
        output shouldContain "public var done: Gen<Boolean>? = null"
        output shouldContain "public var name: Gen<String?>? = null"
        output shouldContain "public var `Refresh-Token`: Gen<Token?>? = null"

        // Every `Gen<…>?` slot is paired with a constant setter so a fixed value needs no `Arb`.
        output shouldContain "public fun id(value: String) {"
        output shouldContain "this.id = Arb.constant(value)"
        output shouldContain "public fun `Refresh-Token`(value: Token?) {"
        output shouldContain "this.`Refresh-Token` = Arb.constant(value)"

        // flush() validates required slots/fields and defaults nullable ones; the wire name
        // stays raw while the Kotlin reference is escaped.
        output shouldContain "PutTodoPathBuilder().apply(path ?: error(\"PutTodo: required `path` block is missing\"))"
        output shouldContain "inner.pathGen(\"id\", pathBuilder.id ?: error(\"PutTodo.path: required `id` is missing\"))"
        output shouldContain "inner.queryGen(\"name\", queryBuilder.name ?: Arb.constant(null))"
        output shouldContain "inner.headerGen(\"Refresh-Token\", headerBuilder.`Refresh-Token` ?: Arb.constant(null))"

        // The scope's only terminal is `buildRequest()` (returns a `Gen<Request>`); sending chains
        // through `Gen<Request>.call()`.
        output shouldContain "public class PutTodoGenerate internal constructor()"
        output shouldContain "public val PutTodo.generate: PutTodoGenerate"
        output shouldContain "public suspend fun request(block: suspend PutTodoScope.() -> Unit): Gen<PutTodo.Request>"
        output shouldContain "public fun buildRequest(): Gen<PutTodo.Request>"
        output shouldContain "public suspend fun Gen<PutTodo.Request>.call(): PutTodo.Response<*> ="
        output shouldNotContain "call(block: suspend PutTodoScope"

        // The 201 variant carries a `TodoDto` body plus `token`/`refreshToken` response headers, so
        // its scope exposes a whole-value body setter and one setter per header field, and builds a Gen.
        output shouldContain "public class PutTodoResponse201Scope internal constructor()"
        output shouldContain "responseCall(PutTodo, PutTodo.Response201::class)"
        output shouldContain "public var body: Gen<TodoDto>? = null"
        output shouldContain "public var token: Gen<Token>? = null"
        output shouldContain "public var refreshToken: Gen<Token?>? = null"
        output shouldContain "token?.let { inner.headerGen(\"token\", it) }"
        output shouldContain "return inner.buildGen() as Gen<PutTodo.Response201>"
        output shouldContain "public fun response201(block: PutTodoResponse201Scope.() -> Unit = {}): Gen<PutTodo.Response201>"
        // A header-less variant (500 → Error) still gets its body setter and builder.
        output shouldContain "public fun response500(block: PutTodoResponse500Scope.() -> Unit = {}): Gen<PutTodo.Response500>"
    }

    @Test
    fun allNullableSlotsAreOptionalNotRequired() {
        // An endpoint whose query (`q`, `limit`) and header (`trace`) fields are *all* nullable, but
        // whose path (`listId`) is required. A slot is required only when it carries at least one
        // non-nullable field, so query/header must be emitted as optional `?.let` blocks while path
        // stays an eager `error(...)`.
        // language=ws
        val source =
            """
            |endpoint SearchTodos GET /todos/{listId: String}
            |    ?{q: String?, limit: Integer?}
            |    #{trace: String?} -> {
            |    200 -> TodoDto
            |}
            |type TodoDto {
            |    id: String
            |}
            """.trimMargin()
        val output = compile(source)(::emitter).shouldBeRight()

        // Path carries a non-nullable field, so it remains required.
        output shouldContain "SearchTodosPathBuilder().apply(path ?: error(\"SearchTodos: required `path` block is missing\"))"

        // Query and header are all-nullable, so they are optional: guarded by `?.let`, never an
        // eager required-block error.
        output shouldContain "query?.let { block ->"
        output shouldContain "header?.let { block ->"
        output shouldNotContain "required `query` block is missing"
        output shouldNotContain "required `header` block is missing"

        // Each nullable field defaults to `Arb.constant(null)` rather than throwing when unset.
        output shouldContain "inner.queryGen(\"q\", queryBuilder.q ?: Arb.constant(null))"
        output shouldContain "inner.queryGen(\"limit\", queryBuilder.limit ?: Arb.constant(null))"
        output shouldContain "inner.headerGen(\"trace\", headerBuilder.trace ?: Arb.constant(null))"
    }

    @Test
    fun emitsPerChannelDsl() {
        // `channel Queue -> String`
        val output = CompileChannelTest.compiler(::emitter).shouldBeRight()

        output shouldContain "channelCall<String>(Queue::class)"
        // The entry point is a `generate` extension property on the generated channel object.
        output shouldContain "public class QueueGenerate internal constructor()"
        output shouldContain "public val Queue.generate: QueueGenerate"

        // Only the send direction is generated; asserting on published messages is left to the
        // test's own broker consumer, so there is no `listen`/receive scope.
        output shouldNotContain "listen"
        output shouldNotContain "QueueListen"
        output shouldNotContain "expecting"
        output shouldNotContain "collecting"
        output shouldNotContain "returning"
        output shouldNotContain "QueueCall"

        // `message` returns a `Gen<Payload>`; publishing chains off its `send()` extension:
        // `Queue.generate.message().send()`. There is no message wrapper class.
        output shouldContain "public fun message(): Gen<String> ="
        output shouldContain "channelCall<String>(Queue::class).messageGen()"
        output shouldContain "public suspend fun Gen<String>.send(topic: String? = null, key: String? = null): String {"
        output shouldContain "return call.send(this)"
        output shouldNotContain "QueueMessage"
        output shouldNotContain "inner.sendFields"
    }

    @Test
    fun emitsPerTypeDslWithSharedReusableBuilder() {
        // `type TodoDto { description: String }`
        val output = CompileMinimalEndpointTest.compiler(::emitter).shouldBeRight()

        // Each record type gets a `<Type>.generate { … }: Gen<…>` entry point — an extension on the
        // type's companion — that pins per-field overrides on the shared builder.
        output shouldContain "public fun TodoDto.Companion.generate(block: TodoDtoBuilder.() -> Unit = {}): Gen<TodoDto> {"
        output shouldContain "return recordGen<TodoDto> {"
        output shouldContain "builder.description?.let { registerPath(\"description\") { it } }"

        // The single reusable `<Type>Builder` carries one `Gen<…>?` var per field.
        output shouldContain "public class TodoDtoBuilder {"
        output shouldContain "public var description: Gen<String>? = null"
        output shouldContain "public fun description(value: String) {"

        // The companion is injected into the model record so the extension has a receiver.
        output shouldContain "companion object"
    }

    @Test
    fun typeBuildersAreSharedNotReplicatedAcrossEndpointBodyAndTypeDsl() {
        // PutTodo's request body is `PotentialTodoDto`, which is also a standalone type. Its builder
        // must be emitted exactly once (by the type DSL) and merely referenced by the endpoint body.
        val output = CompileFullEndpointTest.compiler(::emitter).shouldBeRight()

        // Exactly one declaration of the shared builder (split on the class header → 2 parts).
        output.split("public class PotentialTodoDtoBuilder {").size shouldBe 2
        // Every referenced record type has its own companion `generate`, including the `Error` type.
        output shouldContain "public fun PotentialTodoDto.Companion.generate(block: PotentialTodoDtoBuilder.() -> Unit = {}): Gen<PotentialTodoDto> {"
        output shouldContain "public fun Error.Companion.generate(block: ErrorBuilder.() -> Unit = {}): Gen<Error> {"
        // No per-endpoint body-builder class survives anywhere in the output.
        output shouldNotContain "BodyBuilder"
    }
}
