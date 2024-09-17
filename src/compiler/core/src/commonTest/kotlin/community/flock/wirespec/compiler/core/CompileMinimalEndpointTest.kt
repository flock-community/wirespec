package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileMinimalEndpointTest {

    private val compiler = """
        |endpoint GetTodos GET /todos -> {
        |    200 -> TodoDto[]
        |}
        |type TodoDto {
        |    description: String
        |}
    """.trimMargin().let(::compile)

    @Test
    fun kotlin() {
        val kotlin = """
            |package community.flock.wirespec.generated
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |object GetTodosEndpoint : Wirespec.Endpoint {
            |  data object Path : Wirespec.Path
            |
            |  data object Queries : Wirespec.Queries
            |
            |  data object Headers : Wirespec.Request.Headers
            |
            |  object Request : Wirespec.Request<Unit> {
            |    override val path = Path
            |    override val method = Wirespec.Method.GET
            |    override val queries = Queries
            |    override val headers = Headers
            |    override val body = Unit
            |  }
            |
            |  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = listOf("todos"),
            |      method = request.method.name,
            |      queries = emptyMap(),
            |      headers = emptyMap(),
            |      body = serialization.serialize(request.body, typeOf<Unit>()),
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
            |    Request
            |
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |  sealed interface Response2XX<T: Any> : Response<T>
            |
            |  data class Response200(override val body: List<TodoDto>) : Response2XX<List<TodoDto>> {
            |    override val status = 200
            |    override val headers = Headers
            |    data object Headers : Wirespec.Response.Headers
            |  }
            |
            |  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
            |    when(response) {
            |      is Response200 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = mapOf(),
            |        body = serialization.serialize(response.body, typeOf<List<TodoDto>>()),
            |      )
            |    }
            |
            |  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
            |    when (response.statusCode) {
            |      200 -> Response200(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<List<TodoDto>>()),
            |      )
            |      else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
            |    }
            |
            |  interface Handler: Wirespec.Handler {
            |    suspend fun getTodos(request: Request): Response<*>
            |    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |      override val pathTemplate = "/todos"
            |      override val method = "GET"
            |      override fun server(serialization: Wirespec.Serialization<String>) = object : Wirespec.ServerEdge<Request, Response<*>> {
            |        override fun from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
            |        override fun to(response: Response<*>) = toResponse(serialization, response)
            |      }
            |      override fun client(serialization: Wirespec.Serialization<String>) = object : Wirespec.ClientEdge<Request, Response<*>> {
            |        override fun to(request: Request) = toRequest(serialization, request)
            |        override fun from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
            |      }
            |    }
            |  }
            |}
            |data class TodoDto(
            |  val description: String
            |)
            |
        """.trimMargin()

        compiler(KotlinEmitter()) shouldBeRight kotlin
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public interface GetTodosEndpoint extends Wirespec.Endpoint {
            |  public static class Path implements Wirespec.Path {}
            |
            |  public static class Queries implements Wirespec.Queries {}
            |
            |  public static class RequestHeaders implements Wirespec.Request.Headers {}
            |
            |  public static class Request implements Wirespec.Request<Void> {
            |    private final Path path;
            |    private final Wirespec.Method method;
            |    private final Queries queries;
            |    private final RequestHeaders headers;
            |    private final Void body;
            |    public Request() {
            |      this.path = new Path();
            |      this.method = Wirespec.Method.GET;
            |      this.queries = new Queries();
            |      this.headers = new RequestHeaders();
            |      this.body = null;
            |    }
            |    @Override public Path getPath() { return path; }
            |    @Override public Wirespec.Method getMethod() { return method; }
            |    @Override public Queries getQueries() { return queries; }
            |    @Override public RequestHeaders getHeaders() { return headers; }
            |    @Override public Void getBody() { return body; }
            |  }
            |
            |  public sealed interface Response<T> extends Wirespec.Response<T> {}
            |  public sealed interface Response2XX<T> extends Response<T> {}
            |
            |  public record Response200(@Override java.util.List<TodoDto> body) implements Response2XX<java.util.List<TodoDto>> {
            |    @Override public int getStatus() { return 200; }
            |    @Override public Headers getHeaders() { return new Headers(); }
            |    @Override public java.util.List<TodoDto> getBody() { return body; }
            |    public static class Headers implements Wirespec.Response.Headers {}
            |  }
            |
            |  interface Handler extends Wirespec.Handler {
            |
            |    public static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method.name(),
            |        java.util.List.of("todos"),
            |        java.util.Collections.emptyMap(),
            |        java.util.Collections.emptyMap(),
            |        serialization.serialize(request.getBody(), Wirespec.getType(Void.class, false))
            |      );
            |    }
            |
            |    public static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
            |      return new Request();
            |    }
            |
            |    public static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
            |      return switch (response) {
            |        case Response200 r -> new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, true)));
            |      };
            |    }
            |
            |    public static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
            |      return switch (response.statusCode()) {
            |        case 200 -> new Response200(serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, true)));
            |        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
            |      };
            |    }
            |
            |    java.util.concurrent.CompletableFuture<Response<?>> getTodos(Request request);
            |    public static class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override public String getPathTemplate() { return "/todos"; }
            |      @Override public String getMethod() { return "GET"; }
            |      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization<String> serialization) {
            |        return new Wirespec.ServerEdge<Request, Response<?>>() {
            |          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
            |          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
            |        };
            |      }
            |      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization<String> serialization) {
            |        return new Wirespec.ClientEdge<Request, Response<?>>() {
            |          @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
            |          @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
            |        };
            |      }
            |    }
            |  }
            |}
            |
        """.trimMargin()

        compiler(JavaEmitter()) shouldBeRight java
    }

    @Test
    fun scala() {
        val scala = """
            |package community.flock.wirespec.generated
            |
            |// TODO("Not yet implemented")
            |
            |case class TodoDto(
            |  val description: String
            |)
            |
        """.trimMargin()

        compiler(ScalaEmitter()) shouldBeRight scala
    }

    @Test
    fun typeScript() {
        val ts = """
            |export module Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type RawRequest = { method: Method, path: string[], queries: Record<string, string[]>, headers: Record<string, string[]>, body?: string }
            |  export type RawResponse = { status: number, headers: Record<string, string[]>, body?: string }
            |  export type Content<T> = { type:string, body:T }
            |  export type Request<T> = { path: Record<string, string>, method: Method, query?: Record<string, any>, headers?: Record<string, any>, content?:Content<T> }
            |  export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<any>, RES extends Response<any>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<any>, RES extends Response<any>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |}
            |export module GetTodos {
            |  type Path = {}
            |  type Queries = {}
            |  type Headers = {}
            |  export type Request = { 
            |    path: Path
            |    method: "GET"
            |    queries: Queries
            |    headers: Headers
            |    body: undefined
            |  }
            |  export type Response200 = { 
            |    status: 200
            |    headers: {}
            |    body: TodoDto[]
            |  }
            |  export type Response = Response200
            |  export type Handler = {
            |    getTodos: (request:Request) => Promise<Response>
            |  }
            |  export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    to: (request) => ({
            |      method: "GET",
            |      path: ["todos"],
            |      queries: {},
            |      headers: {},
            |      body: serialization.serialize(request.body)
            |    }),
            |    from: (response) => {
            |      switch (response.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {},
            |            body: serialization.deserialize<TodoDto[]>(response.body)
            |          };
            |        default:
            |          throw new Error(`Cannot internalize response with status: ${'$'}{response.status}`);
            |      }
            |    }
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (request) => {
            |      return {
            |        method: "GET",
            |        path: { 
            |      
            |        },
            |        queries: {
            |  
            |        },
            |        headers: {
            |  
            |        },
            |        body: serialization.deserialize(request.body)
            |      }
            |    },
            |    to: (response) => ({
            |      status: response.status,
            |      headers: {},
            |      body: serialization.serialize(response.body),
            |    })
            |  })
            |}
            |
            |export type TodoDto = {
            |  "description": string
            |}
            |
            |
        """.trimMargin()

        compiler(TypeScriptEmitter()) shouldBeRight ts
    }

    @Test
    fun wirespec() {
        val wirespec = """
            |endpoint GetTodos GET /todos -> {
            |  200 -> TodoDto[]
            |}
            |
            |type TodoDto {
            |  description: String
            |}
            |
        """.trimMargin()

        compiler(WirespecEmitter()) shouldBeRight wirespec
    }
}
