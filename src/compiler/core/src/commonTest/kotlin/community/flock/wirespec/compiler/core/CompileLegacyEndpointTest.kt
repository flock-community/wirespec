package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinLegacyEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileLegacyEndpointTest {

    private val compiledTodo = """
        |endpoint Todo GET /v1/todo ? {done:Boolean} # {auth:String} -> {
        |  200 -> Todo 
        |}
        |type Todo {
        |  name: String,
        |  done: Boolean
        |}
    """.trimMargin().let(::compile)

    private val compiledReqRes = """
        |endpoint Todo POST Request /reqres -> {
        |  200 -> Response 
        |}
        |type Request {
        |  name: String
        |}
        |type Response {
        |  name: String
        |}
    """.trimMargin().let(::compile)

    @Test
    fun testEndpointKotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.Wirespec
            |import kotlin.reflect.typeOf
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
            |      path = "/v1/todo",
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
            |  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<Todo>) : Response200<Todo> {
            |    constructor(body: Todo) : this(
            |      status = 200,
            |      headers = mapOf<String, List<Any?>>(),
            |      content = Wirespec.Content("application/json", body)
            |    )
            |  }
            |  companion object {
            |    const val PATH = "/v1/todo"
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
            |          .read<Todo>(response.content!!, typeOf<Todo>())
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

        compiledTodo(KotlinLegacyEmitter()) shouldBeRight kotlin
    }


    @Test
    fun testEndpointReqResKotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.Wirespec
            |import kotlin.reflect.typeOf
            |
            |interface TodoEndpoint : Wirespec.Endpoint {
            |  sealed interface Request<T> : Wirespec.Request<T>
            |  data class RequestApplicationJson(
            |    override val path: String,
            |    override val method: Wirespec.Method,
            |    override val query: Map<String, List<Any?>>,
            |    override val headers: Map<String, List<Any?>>,
            |    override val content: Wirespec.Content<community.flock.wirespec.generated.Request>
            |  ) : Request<community.flock.wirespec.generated.Request> {
            |    constructor(body: community.flock.wirespec.generated.Request) : this(
            |      path = "/reqres",
            |      method = Wirespec.Method.POST,
            |      query = mapOf<String, List<Any?>>(),
            |      headers = mapOf<String, List<Any?>>(),
            |      content = Wirespec.Content("application/json", body)
            |    )
            |  }
            |
            |  sealed interface Response<T> : Wirespec.Response<T>
            |  sealed interface Response2XX<T> : Response<T>
            |  sealed interface Response200<T> : Response2XX<T>
            |  data class Response200ApplicationJson(override val status: Int, override val headers: Map<String, List<Any?>>, override val content: Wirespec.Content<community.flock.wirespec.generated.Response>) : Response200<community.flock.wirespec.generated.Response> {
            |    constructor(body: community.flock.wirespec.generated.Response) : this(
            |      status = 200,
            |      headers = mapOf<String, List<Any?>>(),
            |      content = Wirespec.Content("application/json", body)
            |    )
            |  }
            |  companion object {
            |    const val PATH = "/reqres"
            |    const val METHOD = "POST"
            |    fun <B> REQUEST_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { request: Wirespec.Request<B> ->
            |      when {
            |        request.content?.type == "application/json" -> contentMapper
            |          .read<community.flock.wirespec.generated.Request>(request.content!!, typeOf<community.flock.wirespec.generated.Request>())
            |          .let { RequestApplicationJson(request.path, request.method, request.query, request.headers, it) }
            |        else -> error("Cannot map request")
            |      }
            |    }
            |    fun <B> RESPONSE_MAPPER(contentMapper: Wirespec.ContentMapper<B>) = { response: Wirespec.Response<B> ->
            |      when {
            |        response.status == 200 && response.content?.type == "application/json" -> contentMapper
            |          .read<community.flock.wirespec.generated.Response>(response.content!!, typeOf<community.flock.wirespec.generated.Response>())
            |          .let { Response200ApplicationJson(response.status, response.headers, it) }
            |        else -> error("Cannot map response with status ${'$'}{response.status}")
            |      }
            |    }
            |  }
            |  suspend fun todo(request: Request<*>): Response<*>
            |}
            |data class Request(
            |  val name: String
            |)
            |data class Response(
            |  val name: String
            |)
            |
        """.trimMargin()

        compiledReqRes(KotlinLegacyEmitter()) shouldBeRight kotlin
    }

    @Test
    fun testEndpointJava() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import java.util.concurrent.CompletableFuture;
            |import java.util.function.Function;
            |
            |public interface TodoEndpoint extends Wirespec.Endpoint {
            |  static String PATH = "/v1/todo";
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
            |      this.path = "/" + "v1" + "/" + "todo";
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
            |    public Response200ApplicationJson(
            |      Todo body
            |    ) {
            |      this.status = 200;
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = new Wirespec.Content("application/json", body);
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

        compiledTodo(JavaEmitter()) shouldBeRight java
    }

    @Test
    fun testEndpointJavaReqRes() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import java.util.concurrent.CompletableFuture;
            |import java.util.function.Function;
            |
            |public interface TodoEndpoint extends Wirespec.Endpoint {
            |  static String PATH = "/reqres";
            |  static String METHOD = "POST";
            |
            |  sealed interface Request<T> extends Wirespec.Request<T> {
            |  }
            |
            |  final class RequestApplicationJson implements Request<community.flock.wirespec.generated.Request> {
            |    private final String path;
            |    private final Wirespec.Method method;
            |    private final java.util.Map<String, java.util.List<Object>> query;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<community.flock.wirespec.generated.Request> content;
            |
            |    public RequestApplicationJson(
            |      String path,
            |      Wirespec.Method method,
            |      java.util.Map<String, java.util.List<Object>> query,
            |      java.util.Map<String, java.util.List<Object>> headers,
            |      Wirespec.Content<community.flock.wirespec.generated.Request> content
            |    ) {
            |      this.path = path;
            |      this.method = method;
            |      this.query = query;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public RequestApplicationJson(
            |      community.flock.wirespec.generated.Request body
            |    ) {
            |      this.path = "/" + "reqres";
            |      this.method = Wirespec.Method.POST;
            |      this.query = java.util.Map.ofEntries();
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = new Wirespec.Content("application/json", body);
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
            |    public Wirespec.Content<community.flock.wirespec.generated.Request> getContent() {
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
            |  final class Response200ApplicationJson implements Response200<community.flock.wirespec.generated.Response> {
            |    private final int status;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<community.flock.wirespec.generated.Response> content;
            |
            |    public Response200ApplicationJson(int status, java.util.Map<String, java.util.List<Object>> headers, Wirespec.Content<community.flock.wirespec.generated.Response> content) {
            |      this.status = status;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public Response200ApplicationJson(
            |      community.flock.wirespec.generated.Response body
            |    ) {
            |      this.status = 200;
            |      this.headers = java.util.Map.ofEntries();
            |      this.content = new Wirespec.Content("application/json", body);
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
            |    public Wirespec.Content<community.flock.wirespec.generated.Response> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> REQUEST_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return request -> {
            |      if (request.getContent().type().equals("application/json")) {
            |        Wirespec.Content<community.flock.wirespec.generated.Request> content = contentMapper.read(request.getContent(), Wirespec.getType(community.flock.wirespec.generated.Request.class, false));
            |        return (Req) new RequestApplicationJson(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    };
            |  }
            |  static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> RESPONSE_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return response -> {
            |      if (response.getStatus() == 200 && response.getContent().type().equals("application/json")) {
            |        Wirespec.Content<community.flock.wirespec.generated.Response> content = contentMapper.read(response.getContent(), Wirespec.getType(community.flock.wirespec.generated.Response.class, false));
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

        compiledReqRes(JavaEmitter()) shouldBeRight java
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
        """.trimMargin()

        compiledTodo(ScalaEmitter()) shouldBeRight scala
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
            |  export const PATH = "/v1/todo"
            |  export const METHOD = "GET"
            |  type RequestUndefined = { path: `/v1/todo`, method: "GET", headers: {  "auth": string}, query: {  "done": boolean} } 
            |  export type Request = RequestUndefined
            |  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: Todo } }
            |  export type Response = Response200ApplicationJson
            |  export type Handler = (request:Request) => Promise<Response>
            |  export type Call = {
            |    todo: Handler
            |  }
            |  export const call = (handler:Handler) => ({METHOD, PATH, handler})
            |  export const requestUndefined = (props:{  "done": boolean,  "auth": string}) => ({path: `/v1/todo`, method: "GET", query: {"done": props.done}, headers: {"auth": props.auth}} as const)
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

        compiledTodo(TypeScriptEmitter()) shouldBeRight ts
    }

    @Test
    fun testEndpointWirespec() {
        val wirespec = """
            |endpoint Todo GET /v1/todo ? {done: Boolean} -> {
            |  200 -> Todo
            |}
            |
            |type Todo {
            |  name: String,
            |  done: Boolean
            |}
            |
        """.trimMargin()

        compiledTodo(WirespecEmitter()) shouldBeRight wirespec
    }
}
