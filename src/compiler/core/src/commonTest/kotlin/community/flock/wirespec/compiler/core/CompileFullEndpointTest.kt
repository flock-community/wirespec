package community.flock.wirespec.compiler.core

import community.flock.wirespec.compiler.core.emit.JavaEmitter
import community.flock.wirespec.compiler.core.emit.KotlinEmitter
import community.flock.wirespec.compiler.core.emit.PythonEmitter
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
        |    201 -> TodoDto #{token: Token}
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
            |package community.flock.wirespec.generated.endpoint
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |import community.flock.wirespec.generated.model.Token
            |import community.flock.wirespec.generated.model.PotentialTodoDto
            |import community.flock.wirespec.generated.model.TodoDto
            |import community.flock.wirespec.generated.model.Error
            |
            |object PutTodo : Wirespec.Endpoint {
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
            |      path = listOf("todos", request.path.id.let{serialization.serialize(it, typeOf<String>())}),
            |      method = request.method.name,
            |      queries = (mapOf("done" to (request.queries.done?.let{ serialization.serializeParam(it, typeOf<Boolean>()) } ?: emptyList()))),
            |      headers = (mapOf("token" to (request.headers.token?.let{ serialization.serializeParam(it, typeOf<Token>()) } ?: emptyList()))),
            |      body = serialization.serialize(request.body, typeOf<PotentialTodoDto>()),
            |    )
            |
            |  fun fromRequest(serialization: Wirespec.Deserializer<String>, request: Wirespec.RawRequest): Request =
            |    Request(
            |      id = serialization.deserialize(request.path[1], typeOf<String>()),
            |      done = serialization.deserializeParam(requireNotNull(request.queries["done"]) { "done is null" }, typeOf<Boolean>()),
            |      token = serialization.deserializeParam(requireNotNull(request.headers["token"]) { "token is null" }, typeOf<Token>()),
            |      body = serialization.deserialize(requireNotNull(request.body) { "body is null" }, typeOf<PotentialTodoDto>()),
            |    )
            |
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |
            |  sealed interface Response2XX<T: Any> : Response<T>
            |  sealed interface Response5XX<T: Any> : Response<T>
            |
            |  sealed interface ResponseTodoDto : Response<TodoDto>
            |  sealed interface ResponseError : Response<Error>
            |
            |  data class Response200(override val body: TodoDto) : Response2XX<TodoDto>, ResponseTodoDto {
            |    override val status = 200
            |    override val headers = ResponseHeaders
            |    data object ResponseHeaders : Wirespec.Response.Headers
            |  }
            |
            |  data class Response201(override val body: TodoDto, val token: Token) : Response2XX<TodoDto>, ResponseTodoDto {
            |    override val status = 201
            |    override val headers = ResponseHeaders(token)
            |    data class ResponseHeaders(
            |      val token: Token,
            |    ) : Wirespec.Response.Headers
            |  }
            |
            |  data class Response500(override val body: Error) : Response5XX<Error>, ResponseError {
            |    override val status = 500
            |    override val headers = ResponseHeaders
            |    data object ResponseHeaders : Wirespec.Response.Headers
            |  }
            |
            |  fun toResponse(serialization: Wirespec.Serializer<String>, response: Response<*>): Wirespec.RawResponse =
            |    when(response) {
            |      is Response200 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = emptyMap(),
            |        body = serialization.serialize(response.body, typeOf<TodoDto>()),
            |      )
            |      is Response201 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = (mapOf("token" to (response.headers.token?.let{ serialization.serializeParam(it, typeOf<Token>()) } ?: emptyList()))),
            |        body = serialization.serialize(response.body, typeOf<TodoDto>()),
            |      )
            |      is Response500 -> Wirespec.RawResponse(
            |        statusCode = response.status,
            |        headers = emptyMap(),
            |        body = serialization.serialize(response.body, typeOf<Error>()),
            |      )
            |    }
            |
            |  fun fromResponse(serialization: Wirespec.Deserializer<String>, response: Wirespec.RawResponse): Response<*> =
            |    when (response.statusCode) {
            |      200 -> Response200(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<TodoDto>()),
            |      )
            |      201 -> Response201(
            |        body = serialization.deserialize(requireNotNull(response.body) { "body is null" }, typeOf<TodoDto>()),
            |        token = serialization.deserializeParam(requireNotNull(response.headers["token"]) { "token is null" }, typeOf<Token>())
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
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class PotentialTodoDto(
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class Token(
            |  val iss: String
            |)
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class TodoDto(
            |  val id: String,
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |package community.flock.wirespec.generated.model
            |
            |import community.flock.wirespec.kotlin.Wirespec
            |import kotlin.reflect.typeOf
            |
            |data class Error(
            |  val code: Long,
            |  val description: String
            |)
            |
        """.trimMargin()

        compiler { KotlinEmitter() } shouldBeRight kotlin
    }

    @Test
    fun java() {
        val java = """
            |package community.flock.wirespec.generated.endpoint;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import community.flock.wirespec.generated.model.Token;
            |import community.flock.wirespec.generated.model.PotentialTodoDto;
            |import community.flock.wirespec.generated.model.TodoDto;
            |import community.flock.wirespec.generated.model.Error;
            |
            |public interface PutTodo extends Wirespec.Endpoint {
            |  public record Path(
            |    String id
            |  ) implements Wirespec.Path {}
            |
            |  public record Queries(
            |    Boolean done
            |  ) implements Wirespec.Queries {}
            |
            |  public record RequestHeaders(
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
            |  sealed interface ResponseTodoDto extends Response<TodoDto> {}
            |  sealed interface ResponseError extends Response<Error> {}
            |
            |  record Response200(TodoDto body) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    @Override public int getStatus() { return 200; }
            |    @Override public Headers getHeaders() { return new Headers(); }
            |    @Override public TodoDto getBody() { return body; }
            |    class Headers implements Wirespec.Response.Headers {}
            |  }
            |  record Response201(Token token, TodoDto body) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    @Override public int getStatus() { return 201; }
            |    @Override public Headers getHeaders() { return new Headers(token); }
            |    @Override public TodoDto getBody() { return body; }
            |    public record Headers(
            |    Token token
            |  ) implements Wirespec.Response.Headers {}
            |  }
            |  record Response500(Error body) implements Response5XX<Error>, ResponseError {
            |    @Override public int getStatus() { return 500; }
            |    @Override public Headers getHeaders() { return new Headers(); }
            |    @Override public Error getBody() { return body; }
            |    class Headers implements Wirespec.Response.Headers {}
            |  }
            |
            |  interface Handler extends Wirespec.Handler {
            |
            |    static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method.name(),
            |        java.util.List.of("todos", serialization.serialize(request.path.id, Wirespec.getType(String.class, false))),
            |        java.util.Map.ofEntries(java.util.Map.entry("done", serialization.serializeParam(request.queries.done, Wirespec.getType(Boolean.class, false)))),
            |        java.util.Map.ofEntries(java.util.Map.entry("token", serialization.serializeParam(request.headers.token, Wirespec.getType(Token.class, false)))),
            |        serialization.serialize(request.getBody(), Wirespec.getType(PotentialTodoDto.class, false))
            |      );
            |    }
            |
            |    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
            |      return new Request(
            |        serialization.<String>deserialize(request.path().get(1), Wirespec.getType(String.class, false)),
            |        java.util.Optional.ofNullable(request.queries().get("done")).map(it -> serialization.<Boolean>deserializeParam(it, Wirespec.getType(Boolean.class, false))).get(),
            |        java.util.Optional.ofNullable(request.headers().get("token")).map(it -> serialization.<Token>deserializeParam(it, Wirespec.getType(Token.class, false))).get(),
            |        serialization.deserialize(request.body(), Wirespec.getType(PotentialTodoDto.class, false))
            |      );
            |    }
            |
            |    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
            |      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, false))); }
            |      if (response instanceof Response201 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Map.ofEntries(java.util.Map.entry("token", serialization.serializeParam(r.getHeaders().token(), Wirespec.getType(Token.class, false)))), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, false))); }
            |      if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(Error.class, false))); }
            |      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
            |    }
            |
            |    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
            |      switch (response.statusCode()) {
            |        case 200: return new Response200(
            |        serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, false))
            |      );
            |        case 201: return new Response201(
            |        java.util.Optional.ofNullable(response.headers().get("token")).map(it -> serialization.<Token>deserializeParam(it, Wirespec.getType(Token.class, false))).get(),
            |        serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, false))
            |      );
            |        case 500: return new Response500(
            |        serialization.deserialize(response.body(), Wirespec.getType(Error.class, false))
            |      );
            |        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
            |      }
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
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record PotentialTodoDto (
            |  String name,
            |  Boolean done
            |) {
            |};
            |
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record Token (
            |  String iss
            |) {
            |};
            |
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoDto (
            |  String id,
            |  String name,
            |  Boolean done
            |) {
            |};
            |
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record Error (
            |  Long code,
            |  String description
            |) {
            |};
            |
        """.trimMargin()

        compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun typeScript() {
        val ts = """
            |export namespace Wirespec {
            |  export type Method = "GET" | "PUT" | "POST" | "DELETE" | "OPTIONS" | "HEAD" | "PATCH" | "TRACE"
            |  export type RawRequest = { method: Method, path: string[], queries: Record<string, string>, headers: Record<string, string>, body?: string }
            |  export type RawResponse = { status: number, headers: Record<string, string>, body?: string }
            |  export type Request<T> = { path: Record<string, unknown>, method: Method, queries?: Record<string, unknown>, headers?: Record<string, unknown>, body?:T }
            |  export type Response<T> = { status:number, headers?: Record<string, unknown>, body?:T }
            |  export type Serialization = { serialize: <T>(type: T) => string; deserialize: <T>(raw: string | undefined) => T }
            |  export type Client<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { to: (request: REQ) => RawRequest; from: (response: RawResponse) => RES }
            |  export type Server<REQ extends Request<unknown>, RES extends Response<unknown>> = (serialization: Serialization) => { from: (request: RawRequest) => REQ; to: (response: RES) => RawResponse }
            |  export type Api<REQ extends Request<unknown>, RES extends Response<unknown>> = { name: string; method: Method, path: string, client: Client<REQ, RES>; server: Server<REQ, RES> }
            |}
            |export namespace PutTodo {
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
            |  export type Response201 = {
            |    status: 201
            |    headers: {"token": Token}
            |    body: TodoDto
            |  }
            |  export type Response500 = {
            |    status: 500
            |    headers: {}
            |    body: Error
            |  }
            |  export type Response = Response200 | Response201 | Response500
            |  export const request = (props: {"id": string, "done": boolean, "token": Token, "body": PotentialTodoDto}): Request => ({
            |    path: {"id": props["id"]},
            |    method: "PUT",
            |    queries: {"done": props["done"]},
            |    headers: {"token": props["token"]},
            |    body: props.body,
            |  })
            |  export const response200 = (props: {"body": TodoDto}): Response200 => ({
            |    status: 200,
            |    headers: {},
            |    body: props.body,
            |  })
            |  export const response201 = (props: {"token": Token, "body": TodoDto}): Response201 => ({
            |    status: 201,
            |    headers: {"token": props["token"]},
            |    body: props.body,
            |  })
            |  export const response500 = (props: {"body": Error}): Response500 => ({
            |    status: 500,
            |    headers: {},
            |    body: props.body,
            |  })
            |  export type Handler = {
            |    putTodo: (request:Request) => Promise<Response>
            |  }
            |  export const client: Wirespec.Client<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    to: (it) => ({
            |      method: "PUT",
            |      path: ["todos", serialization.serialize(it.path["id"])],
            |      queries: {"done": serialization.serialize(it.queries["done"])},
            |      headers: {"token": serialization.serialize(it.headers["token"])},
            |      body: serialization.serialize(it.body)
            |    }),
            |    from: (it) => {
            |      switch (it.status) {
            |        case 200:
            |          return {
            |            status: 200,
            |            headers: {},
            |            body: serialization.deserialize<TodoDto>(it.body)
            |          };
            |        case 201:
            |          return {
            |            status: 201,
            |            headers: {"token": serialization.deserialize(it.headers["token"])},
            |            body: serialization.deserialize<TodoDto>(it.body)
            |          };
            |        case 500:
            |          return {
            |            status: 500,
            |            headers: {},
            |            body: serialization.deserialize<Error>(it.body)
            |          };
            |        default:
            |          throw new Error(`Cannot internalize response with status: ${'$'}{it.status}`);
            |      }
            |    }
            |  })
            |  export const server:Wirespec.Server<Request, Response> = (serialization: Wirespec.Serialization) => ({
            |    from: (it) => {
            |      return {
            |        method: "PUT",
            |        path: { 
            |          "id": serialization.deserialize(it.path[1])
            |        },
            |        queries: {
            |          "done": serialization.deserialize(it.queries["done"])
            |        },
            |        headers: {
            |          "token": serialization.deserialize(it.headers["token"])
            |        },
            |        body: serialization.deserialize(it.body)
            |      }
            |    },
            |    to: (it) => ({
            |      status: it.status,
            |      headers: {},
            |      body: serialization.serialize(it.body),
            |    })
            |  })
            |  export const api = {
            |    name: "putTodo",
            |    method: "PUT",
            |    path: "todos/:id",
            |    server,
            |    client
            |  } as const
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

        compiler { TypeScriptEmitter() } shouldBeRight ts
    }

    @Test
    fun python() {
        val python = """
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class PotentialTodoDto:
            |  name: 'str'
            |  done: 'bool'
            |
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class Token:
            |  iss: 'str'
            |
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class TodoDto:
            |  id: 'str'
            |  name: 'str'
            |  done: 'bool'
            |
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |
            |from ..wirespec import T, Wirespec
            |
            |@dataclass
            |class Error:
            |  code: 'int'
            |  description: 'str'
            |
            |
            |from abc import abstractmethod
            |from dataclasses import dataclass
            |from typing import List, Optional
            |
            |from ..wirespec import T, Wirespec
            |
            |from ..model.Token import Token
            |from ..model.PotentialTodoDto import PotentialTodoDto
            |from ..model.TodoDto import TodoDto
            |from ..model.Error import Error
            |
            |class PutTodo (Wirespec.Endpoint):
            |  @dataclass
            |  class Request(Wirespec.Request[PotentialTodoDto]):
            |    @dataclass
            |    class Path (Wirespec.Request.Path):
            |        id: str 
            |    @dataclass
            |    class Queries (Wirespec.Request.Queries):
            |        done: 'bool'
            |    @dataclass
            |    class Headers (Wirespec.Request.Headers):
            |        token: 'Token'
            | 
            |    @property
            |    def body(self) -> PotentialTodoDto:
            |      return self._body
            |
            |    @property
            |    def path(self) -> Path:
            |      return self._path
            |
            |    @property
            |    def queries(self) -> Queries:
            |      return self._queries
            |
            |    @property
            |    def headers(self) -> Headers:
            |      return self._headers
            |
            |    _body:  PotentialTodoDto
            |    _headers: Headers
            |    _queries: Queries
            |    _path: Path
            |    method: Wirespec.Method = Wirespec.Method.PUT
            |
            |    def __init__(self, id: str, done: bool, token: Token, body: PotentialTodoDto):
            |      self._path = PutTodo.Request.Path(id = id)
            |      self._queries =PutTodo.Request.Queries(  done = done)
            |      self._headers = PutTodo.Request.Headers(  token = token)
            |      self._body = body
            |
            |  @dataclass
            |  class Response200(Wirespec.Response[TodoDto]):
            |    @dataclass
            |    class Headers (Wirespec.Response.Headers): pass
            |
            |    @property
            |    def headers(self) -> Headers:
            |      return self._headers
            |
            |    @property
            |    def body(self) -> TodoDto:
            |      return self._body
            |
            |    _body: TodoDto
            |    _headers: Headers
            |    status: int = 200
            |
            |    def __init__(self, body: TodoDto):
            |      self._headers = PutTodo.Response200.Headers()
            |      self._body = body
            |
            |  @dataclass
            |  class Response201(Wirespec.Response[TodoDto]):
            |    @dataclass
            |    class Headers (Wirespec.Response.Headers):
            |        token: 'Token'
            |
            |    @property
            |    def headers(self) -> Headers:
            |      return self._headers
            |
            |    @property
            |    def body(self) -> TodoDto:
            |      return self._body
            |
            |    _body: TodoDto
            |    _headers: Headers
            |    status: int = 201
            |
            |    def __init__(self, token: Token, body: TodoDto):
            |      self._headers = PutTodo.Response201.Headers(  token = token)
            |      self._body = body
            |
            |  @dataclass
            |  class Response500(Wirespec.Response[Error]):
            |    @dataclass
            |    class Headers (Wirespec.Response.Headers): pass
            |
            |    @property
            |    def headers(self) -> Headers:
            |      return self._headers
            |
            |    @property
            |    def body(self) -> Error:
            |      return self._body
            |
            |    _body: Error
            |    _headers: Headers
            |    status: int = 500
            |
            |    def __init__(self, body: Error):
            |      self._headers = PutTodo.Response500.Headers()
            |      self._body = body
            |
            |  Response = Response200 | Response201 | Response500
            |
            |  class Handler(Wirespec.Endpoint.Handler):
            |    @abstractmethod
            |    def PutTodo(self, req: 'PutTodo.Request') -> 'PutTodo.Response': pass
            |
            |  class Convert(Wirespec.Endpoint.Convert[Request, Response]):
            |    @staticmethod
            |    def to_raw_request(serialization: Wirespec.Serializer, request: 'PutTodo.Request') -> Wirespec.RawRequest:
            |      return Wirespec.RawRequest(
            |        path = ["todos", str(request.path.id)],
            |        method = request.method.value,
            |        queries = {"done": serialization.serialize_param(request.queries.done, bool)},
            |        headers = {"token": serialization.serialize_param(request.headers.token, Token)},
            |        body = serialization.serialize(request.body, PotentialTodoDto),
            |      )
            |
            |    @staticmethod
            |    def from_raw_request(serialization: Wirespec.Deserializer, request: Wirespec.RawRequest) -> 'PutTodo.Request':
            |      return PutTodo.Request(
            |          id = serialization.deserialize(request.path[1], str),
            |    done = serialization.deserialize_param(request.queries.get("done".lower()), bool),
            |    token = serialization.deserialize_param(request.headers.get("token".lower()), Token),
            |          body = serialization.deserialize(request.body, PotentialTodoDto),
            |    )
            |
            |    @staticmethod
            |    def to_raw_response(serialization: Wirespec.Serializer, response: 'PutTodo.Response') -> Wirespec.RawResponse:
            |      match response:
            |        case PutTodo.Response200():
            |          return Wirespec.RawResponse(
            |            status_code = response.status,
            |            headers = {},
            |            body = serialization.serialize(response.body, TodoDto),
            |          )
            |        case PutTodo.Response201():
            |          return Wirespec.RawResponse(
            |            status_code = response.status,
            |            headers = {"token": serialization.serialize_param(response.headers.token, Token)},
            |            body = serialization.serialize(response.body, TodoDto),
            |          )
            |        case PutTodo.Response500():
            |          return Wirespec.RawResponse(
            |            status_code = response.status,
            |            headers = {},
            |            body = serialization.serialize(response.body, Error),
            |          )
            |        case _:
            |          raise Exception("Cannot match response with status: " + str(response.status))
            |    @staticmethod
            |    def from_raw_response(serialization: Wirespec.Deserializer, response: Wirespec.RawResponse) -> 'PutTodo.Response':
            |      match response.status_code:
            |        case 200:
            |          return PutTodo.Response200(
            |            body = serialization.deserialize(response.body, TodoDto),
            |          )
            |        case 201:
            |          return PutTodo.Response201(
            |            body = serialization.deserialize(response.body, TodoDto),
            |            token = serialization.deserialize_param(response.headers.get("token".lower()), Token)
            |          )
            |        case 500:
            |          return PutTodo.Response500(
            |            body = serialization.deserialize(response.body, Error),
            |          )
            |        case _: 
            |          raise Exception("Cannot match response with status: " + str(response.status_code))
            |
            |
            |
            |from . import model
            |from . import endpoint
            |from . import wirespec
            |
        """.trimMargin()
        compiler { PythonEmitter() } shouldBeRight python
    }

    @Test
    fun wirespec() {
        val wirespec = """
            |endpoint PutTodo PUT PotentialTodoDto /todos/{id: String} ? {done: Boolean} -> {
            |  200 -> TodoDto
            |  201 -> TodoDto
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

        compiler { WirespecEmitter() } shouldBeRight wirespec
    }
}
