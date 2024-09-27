package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.ScalaEmitter
import community.flock.wirespec.compiler.core.emit.TypeScriptEmitter
import community.flock.wirespec.compiler.core.emit.WirespecEmitter
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class CompileFullEndpointTest {

    private val compiler = """
        |endpoint PutTodo PUT PotentialTodoDto /todos/{id: String}
        |    ?{done: Boolean}
        |    #{token: Token} -> {
        |    200 -> TodoDto
        |    500 -> Error
        |}
        |type PotentialTodoDto {
        |    name: String,
        |    done: Boolean
        |}
        |type Token {
        |    iss: String
        |}
        |type TodoDto {
        |    id: String,
        |    name: String,
        |    done: Boolean
        |}
        |type Error {
        |    code: Integer,
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
            |object PutTodoEndpoint : Wirespec.Endpoint {
            |  data class Path(
            |    val id: String,
            |  ) : Wirespec.Path
            |
            |  data class Queries(
            |    val done: Boolean,
            |  ) : Wirespec.Queries
            |
            |  data class Headers(
            |    val token: Token,
            |  ) : Wirespec.Request.Headers
            |
            |  class Request(
            |    id: String,
            |    done: Boolean,
            |    token: Token,
            |    override val body: PotentialTodoDto,
            |  ) : Wirespec.Request<PotentialTodoDto> {
            |    override val path = Path(id)
            |    override val method = Wirespec.Method.PUT
            |    override val queries = Queries(done)
            |    override val headers = Headers(token)
            |  }
            |
            |  fun toRequest(serialization: Wirespec.Serializer<String>, request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = listOf("todos", request.path.id.toString()),
            |      method = request.method.name,
            |      queries = listOf(request.queries.done?.let{"done" to serialization.serialize(it, typeOf<Boolean>()).let(::listOf)}).filterNotNull().toMap(),
            |      headers = listOf(request.headers.token?.let{"token" to serialization.serialize(it, typeOf<Token>()).let(::listOf)}).filterNotNull().toMap(),
            |      body = serialization.serialize(request.body, typeOf<PotentialTodoDto>()),
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
            |    Request(
            |      id = serialization.deserialize(request.path[1], typeOf<String>()),
            |      done = serialization.deserialize(requireNotNull(request.queries["done"]?.get(0)) { "done is null" }, typeOf<Boolean>()),
            |      token = serialization.deserialize(requireNotNull(request.headers["token"]?.get(0)) { "token is null" }, typeOf<Token>()),
            |      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<PotentialTodoDto>()),
            |    )
            |
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |  sealed interface Response2XX<T: Any> : Response<T>
            |  sealed interface Response5XX<T: Any> : Response<T>
            |
            |  data class Response200(override val body: TodoDto) : Response2XX<TodoDto> {
            |    override val status = 200
            |    override val headers = Headers
            |    data object Headers : Wirespec.Response.Headers
            |  }
            |  data class Response500(override val body: Error) : Response5XX<Error> {
            |    override val status = 500
            |    override val headers = Headers
            |    data object Headers : Wirespec.Response.Headers
            |  }
            |
            |  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
            |    when(response) {
            |      is Response200 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = mapOf(),
            |        body = serialization.serialize(response.body, typeOf<TodoDto>()),
            |      )
            |      is Response500 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = mapOf(),
            |        body = serialization.serialize(response.body, typeOf<Error>()),
            |      )
            |    }
            |
            |  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
            |    when (response.statusCode) {
            |      200 -> Response200(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<TodoDto>()),
            |      )
            |      500 -> Response500(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<Error>()),
            |      )
            |      else -> error("Cannot match response with status: ${'$'}{response.statusCode}")
            |    }
            |
            |  interface Handler: Wirespec.Handler {
            |    suspend fun putTodo(request: Request): Response<*>
            |    companion object: Wirespec.Server<Request, Response<*>>, Wirespec.Client<Request, Response<*>> {
            |      override val pathTemplate = "/todos/{id}"
            |      override val method = "PUT"
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
            |data class PotentialTodoDto(
            |  val name: String,
            |  val done: Boolean
            |)
            |data class Token(
            |  val iss: String
            |)
            |data class TodoDto(
            |  val id: String,
            |  val name: String,
            |  val done: Boolean
            |)
            |data class Error(
            |  val code: Long,
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
            |public interface PutTodoEndpoint extends Wirespec.Endpoint {
            |  record Path(
            |    String id
            |  ) implements Wirespec.Path {}
            |
            |  record Queries(
            |    Boolean done
            |  ) implements Wirespec.Queries {}
            |
            |  record RequestHeaders(
            |    Token token
            |  ) implements Wirespec.Request.Headers {}
            |
            |  class Request implements Wirespec.Request<PotentialTodoDto> {
            |    private final Path path;
            |    private final Wirespec.Method method;
            |    private final Queries queries;
            |    private final RequestHeaders headers;
            |    private final PotentialTodoDto body;
            |    public Request(String id, Boolean done, Token token, PotentialTodoDto body) {
            |      this.path = new Path(id);
            |      this.method = Wirespec.Method.PUT;
            |      this.queries = new Queries(done);
            |      this.headers = new RequestHeaders(token);
            |      this.body = body;
            |    }
            |    @Override public Path getPath() { return path; }
            |    @Override public Wirespec.Method getMethod() { return method; }
            |    @Override public Queries getQueries() { return queries; }
            |    @Override public RequestHeaders getHeaders() { return headers; }
            |    @Override public PotentialTodoDto getBody() { return body; }
            |  }
            |
            |  sealed interface Response<T> extends Wirespec.Response<T> {}
            |  sealed interface Response2XX<T> extends Response<T> {}
            |  sealed interface Response5XX<T> extends Response<T> {}
            |
            |  record Response200(@Override TodoDto body) implements Response2XX<TodoDto> {
            |    @Override public int getStatus() { return 200; }
            |    @Override public Headers getHeaders() { return new Headers(); }
            |    @Override public TodoDto getBody() { return body; }
            |    public static class Headers implements Wirespec.Response.Headers {}
            |  }
            |  record Response500(@Override Error body) implements Response5XX<Error> {
            |    @Override public int getStatus() { return 500; }
            |    @Override public Headers getHeaders() { return new Headers(); }
            |    @Override public Error getBody() { return body; }
            |    public static class Headers implements Wirespec.Response.Headers {}
            |  }
            |
            |  interface Handler extends Wirespec.Handler {
            |
            |    static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method.name(),
            |        java.util.List.of("todos", request.path.id.toString()),
            |        java.util.Map.of("done", java.util.List.of(serialization.serialize(request.queries.done, Wirespec.getType(Boolean.class, false)))),
            |        java.util.Map.of("token", java.util.List.of(serialization.serialize(request.headers.token, Wirespec.getType(Token.class, false)))),
            |        serialization.serialize(request.getBody(), Wirespec.getType(PotentialTodoDto.class, false))
            |      );
            |    }
            |
            |    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
            |      return new Request(
            |        serialization.deserialize(request.path().get(1), Wirespec.getType(String.class, false)),
            |        serialization.deserialize(request.queries().get("done").get(0), Wirespec.getType(Boolean.class, false)),
            |        serialization.deserialize(request.headers().get("token").get(0), Wirespec.getType(Token.class, false)),
            |        serialization.deserialize(request.body(), Wirespec.getType(PotentialTodoDto.class, false))
            |      );
            |    }
            |
            |    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
            |      return switch (response) {
            |        case Response200 r -> new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, false)));
            |        case Response500 r -> new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(Error.class, false)));
            |      };
            |    }
            |
            |    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
            |      return switch (response.statusCode()) {
            |        case 200 -> new Response200(serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, false)));
            |        case 500 -> new Response500(serialization.deserialize(response.body(), Wirespec.getType(Error.class, false)));
            |        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
            |      };
            |    }
            |
            |    java.util.concurrent.CompletableFuture<Response<?>> putTodo(Request request);
            |    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override public String getPathTemplate() { return "/todos/{id}"; }
            |      @Override public String getMethod() { return "PUT"; }
            |      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization<String> serialization) {
            |        return new Wirespec.ServerEdge<>() {
            |          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
            |          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
            |        };
            |      }
            |      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization<String> serialization) {
            |        return new Wirespec.ClientEdge<>() {
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
            |trait PutTodoEndpoint extends Wirespec.Endpoint {
            |  case class Path(
            |    id: String
            |  ) extends Wirespec.Path
            |
            |  case class Queries(
            |    done: Boolean
            |  ) extends Wirespec.Queries
            |
            |  case class Headers(
            |    token: Token
            |  ) extends Wirespec.Request.Headers
            |
            |  case class Request(
            |    id: String,
            |    done: Boolean,
            |    token: Token,
            |    override val body: PotentialTodoDto
            |  ) extends Wirespec.Request[PotentialTodoDto] {
            |    override val path = Path(id)
            |    override val method = Wirespec.Method.PUT
            |    override val queries = Queries(done)
            |    override val headers = Headers(token)
            |  }
            |
            |  def toRequest(serialization: Wirespec.Serializer[String], request: Request): Wirespec.RawRequest =
            |    Wirespec.RawRequest(
            |      path = List("todos", request.path.id.toString),
            |      method = request.method.name,
            |      queries = List(request.queries.done.let{"done" -> serialization.serialize(it, typeOf[Boolean]).let(List(_))}).filterNotNull().toMap,
            |      headers = List(request.headers.token.let{"token" -> serialization.serialize(it, typeOf[Token]).let(List(_))}).filterNotNull().toMap,
            |      body = serialization.serialize(request.body, typeOf[PotentialTodoDto])
            |    )
            |
            |  def fromRequest(serialization: Wirespec.Deserializer[String], request: Wirespec.RawRequest): Request =
            |    Request(
            |      id = serialization.deserialize(request.path(1), typeOf[String]),
            |      done = serialization.deserialize(requireNotNull(request.queries("done").headOption) { "done is null" }, typeOf[Boolean]),
            |      token = serialization.deserialize(requireNotNull(request.headers("token").headOption) { "token is null" }, typeOf[Token]),
            |      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf[PotentialTodoDto])
            |    )
            |
            |  sealed trait Response[T] extends Wirespec.Response[T]
            |  sealed trait Response2XX[T] extends Response[T]
            |  sealed trait Response5XX[T] extends Response[T]
            |
            |  case class Response200(override val body: TodoDto) extends Response2XX[TodoDto] {
            |    override val status = 200
            |    override val headers = Headers
            |    case object Headers extends Wirespec.Response.Headers
            |  }
            |  case class Response500(override val body: Error) extends Response5XX[Error] {
            |    override val status = 500
            |    override val headers = Headers
            |    case object Headers extends Wirespec.Response.Headers
            |  }
            |
            |  def toResponse(serialization: Wirespec.Serializer[String], response: Response[_]): Wirespec.RawResponse =
            |    response match {
            |      case r: Response200 => Wirespec.RawResponse(
            |        statusCode = r.status,
            |        headers = Map.empty,
            |        body = serialization.serialize(r.body, typeOf[TodoDto])
            |      )
            |      case r: Response500 => Wirespec.RawResponse(
            |        statusCode = r.status,
            |        headers = Map.empty,
            |        body = serialization.serialize(r.body, typeOf[Error])
            |      )
            |    }
            |
            |  def fromResponse(serialization: Wirespec.Deserializer[String], response: Wirespec.RawResponse): Response[_] =
            |    response.statusCode match {
            |      case 200 => Response200(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf[TodoDto])
            |      )
            |      case 500 => Response500(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf[Error])
            |      )
            |      case _ => throw new IllegalStateException(s"Cannot match response with status: ${'$'}{response.statusCode}")
            |    }
            |
            |  trait Handler extends Wirespec.Handler {
            |    def putTodo(request: Request): Response[_]
            |    object Handler extends Wirespec.Server[Request, Response[_]] with Wirespec.Client[Request, Response[_]] {
            |      override val pathTemplate = "/todos/{id}"
            |      override val method = "PUT"
            |      override def server(serialization: Wirespec.Serialization[String]) = new Wirespec.ServerEdge[Request, Response[_]] {
            |        override def from(request: Wirespec.RawRequest) = fromRequest(serialization, request)
            |        override def to(response: Response[_]) = toResponse(serialization, response)
            |      }
            |      override def client(serialization: Wirespec.Serialization[String]) = new Wirespec.ClientEdge[Request, Response[_]] {
            |        override def to(request: Request) = toRequest(serialization, request)
            |        override def from(response: Wirespec.RawResponse) = fromResponse(serialization, response)
            |      }
            |    }
            |  }
            |}
            |
            |case class PotentialTodoDto(
            |  name: String,
            |  done: Boolean
            |)
            |
            |case class Token(
            |  iss: String
            |)
            |
            |case class TodoDto(
            |  id: String,
            |  name: String,
            |  done: Boolean
            |)
            |
            |case class Error(
            |  code: Long,
            |  description: String
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
            |  export type Request<T> = { path: Record<string, unknown>, method: Method, query?: Record<string, unknown>, headers?: Record<string, unknown>, content?:Content<T> }
            |  export type Response<T> = { status:number, headers?: Record<string, unknown[]>, content?:Content<T> }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |}
            |export module PutTodo {
            |  type Path = {
            |    "id": string,
            |  }
            |  type Queries = {
            |    "done": boolean,
            |  }
            |  type Headers = {
            |    "token": Token,
            |  }
            |  export type Request = { 
            |    path: Path
            |    method: "PUT"
            |    queries: Queries
            |    headers: Headers
            |    body: PotentialTodoDto
            |  }
            |  export type Response200 = { 
            |    status: 200
            |    headers: {}
            |    body: TodoDto
            |  }
            |  export type Response500 = { 
            |    status: 500
            |    headers: {}
            |    body: Error
            |  }
            |  export type Response = Response200 | Response500
            |  export type Handler = {
            |    putTodo: (request:Request) => Promise<Response>
            |  }
            |  export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    to: (request) => ({
            |      method: "PUT",
            |      path: ["todos", serialization.serialize(request.path.id)],
            |      queries: {done: [serialization.serialize(request.queries.done)]},
            |      headers: {token: [serialization.serialize(request.headers.token)]},
            |      body: serialization.serialize(request.body)
            |    }),
            |    from: (response) => {
            |      switch (response.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {},
            |            body: serialization.deserialize<TodoDto>(response.body)
            |          };
            |        case 500:
            |          return {
            |            status: 500,
            |            headers: {},
            |            body: serialization.deserialize<Error>(response.body)
            |          };
            |        default:
            |          throw new Error(`Cannot internalize response with status: ${'$'}{response.status}`);
            |      }
            |    }
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (request) => {
            |      return {
            |        method: "PUT",
            |        path: { 
            |          id: serialization.deserialize(request.path[1])
            |        },
            |        queries: {
            |          done: serialization.deserialize(request.queries.done[0])
            |        },
            |        headers: {
            |          token: serialization.deserialize(request.headers.token[0])
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
            |export type PotentialTodoDto = {
            |  "name": string,
            |  "done": boolean
            |}
            |
            |
            |export type Token = {
            |  "iss": string
            |}
            |
            |
            |export type TodoDto = {
            |  "id": string,
            |  "name": string,
            |  "done": boolean
            |}
            |
            |
            |export type Error = {
            |  "code": number,
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
            |endpoint PutTodo PUT PotentialTodoDto /todos/{id: String} ? {done: Boolean} -> {
            |  200 -> TodoDto
            |  500 -> Error
            |}
            |
            |type PotentialTodoDto {
            |  name: String,
            |  done: Boolean
            |}
            |
            |type Token {
            |  iss: String
            |}
            |
            |type TodoDto {
            |  id: String,
            |  name: String,
            |  done: Boolean
            |}
            |
            |type Error {
            |  code: Integer,
            |  description: String
            |}
            |
        """.trimMargin()

        compiler(WirespecEmitter()) shouldBeRight wirespec
    }
}
