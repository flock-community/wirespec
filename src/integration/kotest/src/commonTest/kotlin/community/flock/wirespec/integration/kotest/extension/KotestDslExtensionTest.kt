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

    private fun emitter(): Emitter = KotlinIrEmitter(pkg, EmitShared(false)).applyExtensions(listOf(KotestDslExtension(pkg)))

    @Test
    fun emitsPerEndpointDslWithGenerateExtension() {
        val output = CompileMinimalEndpointTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public class GetTodosScope internal constructor()"
        output shouldContain "endpointCall(GetTodos.Handler, GetTodos)"
        output shouldContain "public class GetTodosGenerate internal constructor()"
        output shouldContain "public val GetTodos.generate: GetTodosGenerate"

        output shouldContain "public suspend fun request(block: suspend GetTodosScope.() -> Unit): Arb<GetTodos.Request>"
        output shouldContain "public fun buildRequest(): Arb<GetTodos.Request>"
        output shouldContain "return inner.buildRequestGen()"

        output shouldContain "public suspend fun Gen<GetTodos.Request>.call(): GetTodos.Response<*> ="
        output shouldContain "requestCall(GetTodos.Handler, GetTodos, this)"
        output shouldNotContain "call(block: suspend GetTodosScope"
        output shouldNotContain "expectingClass"

        output shouldContain "public class GetTodosResponse200Scope internal constructor()"
        output shouldContain "responseCall(GetTodos, GetTodos.Response200::class)"
        output shouldContain "public var body: Gen<List<TodoDto>>? = null"
        output shouldContain "public fun response200(block: GetTodosResponse200Scope.() -> Unit = {}): Arb<GetTodos.Response200>"

        output shouldContain "public suspend fun Gen<GetTodos.Response<*>>.mock(predicate: (GetTodos.Request) -> Boolean): Unit ="
        output shouldContain "responseMock(GetTodos.Handler, this, predicate)"
    }

    @Test
    fun blockStyleSlotsAreVarsValidatedOnFlush() {
        val output = CompileFullEndpointTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public class PutTodoScope internal constructor()"
        output shouldContain "private var path: (PutTodoPathBuilder.() -> Unit)? = null"
        output shouldContain "public fun path(block: PutTodoPathBuilder.() -> Unit) {"
        output shouldContain "this.path = block"
        output shouldContain "private var query: (PutTodoQueryBuilder.() -> Unit)? = null"
        output shouldContain "private var header: (PutTodoHeaderBuilder.() -> Unit)? = null"
        output shouldContain "private var body: (PotentialTodoDtoBuilder.() -> Unit)? = null"
        output shouldContain "public fun body(block: PotentialTodoDtoBuilder.() -> Unit) {"
        output shouldContain "this.body = block"
        output shouldNotContain "PutTodoPotentialTodoDtoBodyBuilder"

        output shouldContain "public class PutTodoPathBuilder {"
        output shouldContain "public var id: Gen<String>? = null"
        output shouldContain "public var done: Gen<Boolean>? = null"
        output shouldContain "public var name: Gen<String?>? = null"
        output shouldContain "public var `Refresh-Token`: Gen<Token?>? = null"

        output shouldContain "public fun id(value: String) {"
        output shouldContain "this.id = Arb.constant(value)"
        output shouldContain "public fun `Refresh-Token`(value: Token?) {"
        output shouldContain "this.`Refresh-Token` = Arb.constant(value)"

        output shouldContain "PutTodoPathBuilder().apply(path ?: error(\"PutTodo: required `path` block is missing\"))"
        output shouldContain "inner.pathGen(\"id\", pathBuilder.id ?: error(\"PutTodo.path: required `id` is missing\"))"
        output shouldContain "inner.queryGen(\"name\", queryBuilder.name ?: Arb.constant(null))"
        output shouldContain "inner.headerGen(\"Refresh-Token\", headerBuilder.`Refresh-Token` ?: Arb.constant(null))"

        output shouldContain "public class PutTodoGenerate internal constructor()"
        output shouldContain "public val PutTodo.generate: PutTodoGenerate"
        output shouldContain "public suspend fun request(block: suspend PutTodoScope.() -> Unit): Arb<PutTodo.Request>"
        output shouldContain "public fun buildRequest(): Arb<PutTodo.Request>"
        output shouldContain "public suspend fun Gen<PutTodo.Request>.call(): PutTodo.Response<*> ="
        output shouldNotContain "call(block: suspend PutTodoScope"

        output shouldContain "public class PutTodoResponse201Scope internal constructor()"
        output shouldContain "responseCall(PutTodo, PutTodo.Response201::class)"
        output shouldContain "public var body: Gen<TodoDto>? = null"
        output shouldContain "public var token: Gen<Token>? = null"
        output shouldContain "public var refreshToken: Gen<Token?>? = null"
        output shouldContain "token?.let { inner.headerGen(\"token\", it) }"
        output shouldContain "return inner.buildGen() as Arb<PutTodo.Response201>"
        output shouldContain "public fun response201(block: PutTodoResponse201Scope.() -> Unit = {}): Arb<PutTodo.Response201>"
        output shouldContain "public fun response500(block: PutTodoResponse500Scope.() -> Unit = {}): Arb<PutTodo.Response500>"
    }

    @Test
    fun allNullableSlotsAreOptionalNotRequired() {
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

        output shouldContain "SearchTodosPathBuilder().apply(path ?: error(\"SearchTodos: required `path` block is missing\"))"

        output shouldContain "query?.let { block ->"
        output shouldContain "header?.let { block ->"
        output shouldNotContain "required `query` block is missing"
        output shouldNotContain "required `header` block is missing"

        output shouldContain "inner.queryGen(\"q\", queryBuilder?.q ?: Arb.constant(null))"
        output shouldContain "inner.queryGen(\"limit\", queryBuilder?.limit ?: Arb.constant(null))"
        output shouldContain "inner.headerGen(\"trace\", headerBuilder?.trace ?: Arb.constant(null))"
    }

    @Test
    fun emitsPerChannelDsl() {
        val output = CompileChannelTest.compiler(::emitter).shouldBeRight()

        output shouldContain "channelCall<String>(Queue::class)"
        output shouldContain "public class QueueGenerate internal constructor()"
        output shouldContain "public val Queue.generate: QueueGenerate"

        output shouldNotContain "listen"
        output shouldNotContain "QueueListen"
        output shouldNotContain "expecting"
        output shouldNotContain "collecting"
        output shouldNotContain "returning"
        output shouldNotContain "QueueCall"

        output shouldContain "public fun message(): Arb<String> ="
        output shouldContain "channelCall<String>(Queue::class).messageGen()"
        output shouldContain "public suspend fun Gen<String>.send(topic: String? = null, key: String? = null): String {"
        output shouldContain "return call.send(this)"
        output shouldNotContain "QueueMessage"
        output shouldNotContain "inner.sendFields"
    }

    @Test
    fun channelsSharingAPayloadEmitOneSendExtension() {
        val source =
            // language=ws
            """
            |type Event {
            |  id: String
            |}
            |channel NewListener -> Event
            |channel OldListener -> Event
            """.trimMargin()
        val output = compile(source)(::emitter).shouldBeRight()

        output shouldContain "public val NewListener.generate: NewListenerGenerate"
        output shouldContain "public val OldListener.generate: OldListenerGenerate"
        val sendSignature = "public suspend fun Gen<Event>.send(topic: String? = null, key: String? = null): Event {"
        output.windowed(sendSignature.length).count { it == sendSignature } shouldBe 1
    }

    @Test
    fun primitiveBodyGetsWholeValueSlot() {
        // language=ws
        val source =
            """
            |endpoint ImportContacts POST Bytes /contacts -> {
            |    200 -> String
            |}
            """.trimMargin()
        val output = compile(source)(::emitter).shouldBeRight()

        output shouldContain "public class ImportContactsScope internal constructor()"
        output shouldContain "public var body: Gen<ByteArray>? = null"
        output shouldContain "public fun body(value: ByteArray) {"
        output shouldContain "this.body = Arb.constant(value)"
        output shouldContain "inner.bodyTransform { _, rs -> gen.draw(rs) }"
        output shouldNotContain "ByteArrayBuilder"
    }

    @Test
    fun underscoredNamesReferenceThePascalCasedDeclarations() {
        val source =
            // language=ws
            """
            |type Event_Payload {
            |  id: String
            |}
            |channel Publish_Event -> Event_Payload
            """.trimMargin()
        val output = compile(source)(::emitter).shouldBeRight()

        output shouldNotContain "Publish_Event"
        output shouldNotContain "Event_Payload"

        output shouldContain "import com.example.api.channel.PublishEvent"
        output shouldContain "public class PublishEventGenerate internal constructor()"
        output shouldContain "public val PublishEvent.generate: PublishEventGenerate"
        output shouldContain "get() = PublishEventGenerate()"
        output shouldContain "channelCall<EventPayload>(PublishEvent::class)"
        output shouldContain "val builder = EventPayloadBuilder().apply(block)"
        output shouldContain "public class EventPayloadBuilder {"
    }

    @Test
    fun emitsPerTypeDslWithSharedReusableBuilder() {
        val output = CompileMinimalEndpointTest.compiler(::emitter).shouldBeRight()

        output shouldContain "public fun TodoDto.Companion.generate(block: TodoDtoBuilder.() -> Unit = {}): Arb<TodoDto> {"
        output shouldContain "return recordGen<TodoDto> {"
        output shouldContain "builder.description?.let { registerPath(\"description\") { it } }"

        output shouldContain "public class TodoDtoBuilder {"
        output shouldContain "public var description: Gen<String>? = null"
        output shouldContain "public fun description(value: String) {"

        output shouldContain "companion object"
    }

    @Test
    fun typeBuildersAreSharedNotReplicatedAcrossEndpointBodyAndTypeDsl() {
        val output = CompileFullEndpointTest.compiler(::emitter).shouldBeRight()

        output.split("public class PotentialTodoDtoBuilder {").size shouldBe 2
        output shouldContain "public fun PotentialTodoDto.Companion.generate(block: PotentialTodoDtoBuilder.() -> Unit = {}): Arb<PotentialTodoDto> {"
        output shouldContain "public fun Error.Companion.generate(block: ErrorBuilder.() -> Unit = {}): Arb<Error> {"
        output shouldNotContain "BodyBuilder"
    }
}
