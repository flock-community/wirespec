package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import community.flock.wirespec.compiler.core.emit.common.Emitter
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test
import kotlin.test.assertEquals

class CompileEndpoointTest {

    private val logger = noLogger

    private val compiler = compile(
        """
        endpoint Todo GET /todo ? {done:Boolean} # {auth:String} -> {
          200 -> Todo 
        }
        type Todo {
          name: String,
          done: Boolean
        }
        """.trimIndent()
    )

    @Test
    fun testEnumKotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.Wirespec
            |
            |interface TodoEndpoint :  {
            |  sealed interface Request<T> : Wirespec.Request<T>
            |  data class RequestApplicationJson(
            |    val path: String,
            |    val method: Wirespec.Method,
            |    val query: Map<String, List<Any?>>,
            |    val headers: Map<String, List<Any?>>,
            |    val content: Wirespec.Content<Unit>? = null
            |  ) :  {
            |    constructor(done: Boolean, auth: String) : this(
            |      path = "/todo",
            |      method = Wirespec.Method.GET,
            |      query = mapOf<String, List<Any?>>("done" to done),
            |      headers = mapOf<String, List<Any?>>("auth" to auth),
            |      content = null
            |    )
            |  }
            |
            |  sealed interface Response<T> : Wirespec.Response<T>
            |  sealed interface Response200<T> : Response<T>
            |  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Todo>? = null) : Response200<Todo> {
            |    constructor(headers: Map<String, List<Any?>>, body: Todo): this(
            |      status = 200,
            |      headers = headers,
            |      content = Wirespec.Content("application/json", body),
            |    )
            |  }
            |  companion object {
            |    const val PATH = "/todo"
            |    const val METHOD = "GET"
            |    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
            |      when {
            |        request.content?.type == "application/json" -> contentMapper
            |          .read<Pet>(request.content!!, Wirespec.getType(Unit::class.java, false))
            |          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
            |        else -> error("Cannot map request")
            |      }
            |    }
            |    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
            |      when {
            |        response.status == 200 && response.content?.type == "application/json" -> contentMapper
            |          .read<Pet>(response.content!!, Wirespec.getType(Pet::class.java, false))
            |          .let { Response200ApplicationJson(response.headers, it.body) }
            |        else -> error("Cannot map response with status ${'$'}{response.status}")
            |      }
            |    }
            |  }
            |}
            |data class Todo(
            |  val name: String,
            |  val done: Boolean
            |)
            |
        """.trimMargin()

        assertEquals(kotlin, compiler(KotlinEmitter(logger = logger)).getOrNull())

        compiler(KotlinEmitter(logger = logger)) shouldBeRight kotlin
    }

    @Test
    fun testEnumJava() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.Wirespec;
            |
            |public enum MyAwesomeEnum implements Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE");
            |  public final String label;
            |  MyAwesomeEnum(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |}
            |
        """.trimMargin()

        assertEquals(java, compiler(JavaEmitter(logger = logger)).getOrNull())

        compiler(JavaEmitter(logger = logger)) shouldBeRight java
    }

    @Test
    fun testEnumScala() {
        val scala = """
            package community.flock.wirespec.generated
            
            sealed abstract class MyAwesomeEnum(val label: String)
            object MyAwesomeEnum {
              final case object ONE extends MyAwesomeEnum(label = "ONE")
              final case object TWO extends MyAwesomeEnum(label = "Two")
              final case object THREE_MORE extends MyAwesomeEnum(label = "THREE_MORE")
            }

        """.trimIndent()

        compiler(ScalaEmitter(logger = logger)) shouldBeRight scala
    }

    @Test
    fun testEnumTypeScript() {
        val ts = """
            export type MyAwesomeEnum = "ONE" | "Two" | "THREE_MORE"

        """.trimIndent()

        compiler(TypeScriptEmitter(logger = logger)) shouldBeRight ts
    }

    @Test
    fun testEnumWirespec() {
        val wirespec = """
            enum MyAwesomeEnum {
              ONE, Two, THREE_MORE
            }
        
        """.trimIndent()

        compiler(WirespecEmitter(logger = logger)) shouldBeRight wirespec
    }

    private fun compile(source: String) = { emitter: Emitter ->
        Wirespec.compile(source)(logger)(emitter)
            .map { it.first().result }
            .onLeft(::println)
    }
}
