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

class CompileEndpointTest {

    private val logger = noLogger

    private val compiler = compile(
        """
        |endpoint Todo GET /todo ? {done:Boolean} # {auth:String} -> {
        |  200 -> Todo 
        |}
        |type Todo {
        |  name: String,
        |  done: Boolean
        |}
        """.trimMargin()
    )

    @Test
    fun testEndpointKotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.Wirespec
            |
            |interface TodoEndpoint : Wirespec.Endpoint {
            |  sealed interface Request<T> : Wirespec.Request<T>
            |  data class RequestUnit(
            |    override val path: String,
            |    override val method: Wirespec.Method,
            |    override val query: Map<String, List<Any?>>,
            |    override val headers: Map<String, List<Any?>>,
            |    override val content: Wirespec.Content<Unit>? = null
            |  ) : Request<Unit> {
            |    constructor(done: Boolean, auth: String) : this(
            |      path = "/todo",
            |      method = Wirespec.Method.GET,
            |      query = mapOf<String, List<Any?>>("done" to listOf(done)),
            |      headers = mapOf<String, List<Any?>>("auth" to listOf(auth)),
            |      content = null
            |    )
            |  }
            |
            |  sealed interface Response<T> : Wirespec.Response<T>
            |  sealed interface Response2XX<T> : Response<T>
            |  sealed interface Response200<T> : Response2XX<T>
            |  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Todo>? = null) : Response200<Todo>
            |  companion object {
            |    const val PATH = "/todo"
            |    const val METHOD = "GET"
            |    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
            |      when {
            |        request.content == null -> RequestUnit(request.path, request.method, request.query, request.headers)
            |        else -> error("Cannot map request")
            |      }
            |    }
            |    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
            |      when {
            |        response.status == 200 && response.content?.type == "application/json" -> contentMapper
            |          .read<Todo>(response.content!!, Wirespec.getType(Todo::class.java, false))
            |          .let { Response200ApplicationJson(response.status, response.headers, it) }
            |        else -> error("Cannot map response with status ${'$'}{response.status}")
            |      }
            |    }
            |  }
            |  suspend fun todo(request: Request<*>): Response<*>
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
    fun testEndpointJava() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.Wirespec;
            |
            |import java.util.concurrent.CompletableFuture;
            |import java.util.function.Function;
            |
            |public interface TodoEndpoint extends Wirespec.Endpoint {
            |  static String PATH = "/todo";
            |  static String METHOD = "GET";
            |
            |  sealed interface Request<T> extends Wirespec.Request<T> {
            |  }
            |
            |  final class RequestUnit implements Request<Void> {
            |    private final String path;
            |    private final Wirespec.Method method;
            |    private final java.util.Map<String, java.util.List<Object>> query;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Void> content;
            |
            |    public RequestUnit(
            |      String path,
            |      Wirespec.Method method,
            |      java.util.Map<String, java.util.List<Object>> query,
            |      java.util.Map<String, java.util.List<Object>> headers,
            |      Wirespec.Content<Void> content
            |    ) {
            |      this.path = path;
            |      this.method = method;
            |      this.query = query;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public RequestUnit(
            |      Boolean done,
            |      String auth
            |    ) {
            |      this.path = "/" + "todo";
            |      this.method = Wirespec.Method.GET;
            |      this.query = java.util.Map.ofEntries(java.util.Map.entry("done", java.util.List.of(done)));
            |      this.headers = java.util.Map.ofEntries(java.util.Map.entry("auth", java.util.List.of(auth)));
            |      this.content = null;
            |    }
            |
            |    @Override
            |    public String getPath() {
            |      return path;
            |    }
            |
            |    @Override
            |    public Wirespec.Method getMethod() {
            |      return method;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getQuery() {
            |      return query;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Void> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  sealed interface Response<T> extends Wirespec.Response<T> {
            |  };
            |
            |  sealed interface Response2XX<T> extends Response<T> {
            |  };
            |
            |  sealed interface Response200<T> extends Response2XX<T> {
            |  };
            |
            |  final class Response200ApplicationJson implements Response200<Todo> {
            |    private final int status;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Todo> content;
            |
            |    public Response200ApplicationJson(int status, java.util.Map<String, java.util.List<Object>> headers, Wirespec.Content<Todo> content) {
            |      this.status = status;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    @Override
            |    public int getStatus() {
            |      return status;
            |    }
            |
            |    @Override
            |    public java.util.Map<String, java.util.List<Object>> getHeaders() {
            |      return headers;
            |    }
            |
            |    @Override
            |    public Wirespec.Content<Todo> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> REQUEST_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return request -> {
            |      if (request.getContent() == null) {
            |        return (Req) new RequestUnit(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), null);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    };
            |  }
            |  static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> RESPONSE_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return response -> {
            |      if (response.getStatus() == 200 && response.getContent().type().equals("application/json")) {
            |        Wirespec.Content<Todo> content = contentMapper.read(response.getContent(), Wirespec.getType(Todo.class, false));
            |        return (Res) new Response200ApplicationJson(response.getStatus(), response.getHeaders(), content);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    };
            |  }
            |
            |  public CompletableFuture<Response<?>> todo(Request<?> request);
            |
            |}
            |
        """.trimMargin()

        assertEquals(java, compiler(JavaEmitter(logger = logger)).getOrNull())

        compiler(JavaEmitter(logger = logger)) shouldBeRight java
    }

    @Test
    fun testEnumScala() {
        val scala = """
            |package community.flock.wirespec.generated
            |
            |// TODO("Not yet implemented")
            |
            |case class Todo(
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |
        """.trimMargin()

        compiler(ScalaEmitter(logger = logger)) shouldBeRight scala
    }

    @Test
    fun testEndpointTypeScript() {
        val ts = """
            |export module Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type Content<T> = { type:string, body:T }
            |  export type Request<T> = { path:string, method: Method, query?: Record<string, any[]>, headers?: Record<string, any[]>, content?:Content<T> }
            |  export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
            |}
            |export module Todo {
            |  export const PATH = "/todo"
            |  export const METHOD = "GET"
            |  type RequestUndefined = { path: `/todo`, method: "GET", headers: {  "auth": string}, query: {  "done": boolean} } 
            |  export type Request = RequestUndefined
            |  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo } }
            |  export type Response = Response200ApplicationJson
            |  export type Handler = (request:Request) => Promise<Response>
            |  export type Call = {
            |    todo: Handler
            |  }
            |  export const requestUndefined = (props:{  "done": boolean,  "auth": string}) => ({path: `/todo`, method: "GET", query: {"done": props.done}, headers: {"auth": props.auth}} as const)
            |  export const response200ApplicationJson = (props:{  "body": Todo}) => ({status: 200, headers: {}, content: {type: "application/json", body: props.body}} as const)
            |}
            |
            |export type Todo = {
            |  "name": string,
            |  "done": boolean
            |}
            |
            |
        """.trimMargin()

        compiler(TypeScriptEmitter(logger = logger)) shouldBeRight ts
    }

    @Test
    fun testEndpointWirespec() {
        val wirespec = """
            |endpoint Todo GET /todo ? {done: Boolean} -> {
            |  200 -> Todo
            |}
            |
            |type Todo {
            |  name: String,
            |  done: Boolean
            |}
            |
        """.trimMargin()

        compiler(WirespecEmitter(logger = logger)) shouldBeRight wirespec
    }

    private fun compile(source: String) = { emitter: Emitter ->
        Wirespec.compile(source)(logger)(emitter)
            .map { it.first().result }
            .onLeft(::println)
    }
}
