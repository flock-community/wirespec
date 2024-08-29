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
            |import community.flock.wirespec.Wirespec
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
            |  sealed interface Request<T: Any> : Wirespec.Request<T>
            |
            |  data class RequestApplicationJson(
            |    override val path: Path,
            |    override val method: Wirespec.Method,
            |    override val queries: Queries,
            |    override val headers: Headers,
            |    override val body: PotentialTodoDto,
            |  ) : Request<PotentialTodoDto> {
            |    constructor(id: String, done: Boolean, token: Token, body: PotentialTodoDto) : this(
            |      path = Path(id),
            |      method = Wirespec.Method.PUT,
            |      queries = Queries(done),
            |      headers = Headers(token),
            |      body = body,
            |    )
            |  }
            |
            |  sealed interface Response<T: Any> : Wirespec.Response<T>
            |  sealed interface Response2XX<T: Any> : Response<T>
            |  sealed interface Response5XX<T: Any> : Response<T>
            |
            |  data class Response200ApplicationJson(override val body: TodoDto) : Response2XX<TodoDto> {
            |    override val status = 200
            |    override val headers = Headers
            |    data object Headers : Wirespec.Response.Headers
            |  }
            |  data class Response500ApplicationJson(override val body: Error) : Response5XX<Error> {
            |    override val status = 500
            |    override val headers = Headers
            |    data object Headers : Wirespec.Response.Headers
            |  }
            |
            |  const val PATH_TEMPLATE = "/todos/{id}"
            |  const val METHOD_VALUE = "PUT"
            |
            |  interface Handler {
            |    suspend fun putTodo(request: Request<*>): Response<*>
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
            |import community.flock.wirespec.Wirespec;
            |
            |import java.util.concurrent.CompletableFuture;
            |import java.util.function.Function;
            |
            |public interface PutTodoEndpoint extends Wirespec.Endpoint {
            |  static String PATH = "/todos/{id}";
            |  static String METHOD = "PUT";
            |
            |  sealed interface Request<T> extends Wirespec.Request<T> {
            |  }
            |
            |  final class RequestApplicationJson implements Request<PotentialTodoDto> {
            |    private final String path;
            |    private final Wirespec.Method method;
            |    private final java.util.Map<String, java.util.List<Object>> query;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<PotentialTodoDto> content;
            |
            |    public RequestApplicationJson(
            |      String path,
            |      Wirespec.Method method,
            |      java.util.Map<String, java.util.List<Object>> query,
            |      java.util.Map<String, java.util.List<Object>> headers,
            |      Wirespec.Content<PotentialTodoDto> content
            |    ) {
            |      this.path = path;
            |      this.method = method;
            |      this.query = query;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public RequestApplicationJson(
            |      String id,
            |      Boolean done,
            |      Token token,
            |      PotentialTodoDto body
            |    ) {
            |      this.path = "/" + "todos" + "/" + id;
            |      this.method = Wirespec.Method.PUT;
            |      this.query = java.util.Map.ofEntries(java.util.Map.entry("done", java.util.List.of(done)));
            |      this.headers = java.util.Map.ofEntries(java.util.Map.entry("token", java.util.List.of(token)));
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
            |    public Wirespec.Content<PotentialTodoDto> getContent() {
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
            |  sealed interface Response5XX<T> extends Response<T> {
            |  };
            |
            |  sealed interface Response200<T> extends Response2XX<T> {
            |  };
            |
            |  sealed interface Response500<T> extends Response5XX<T> {
            |  };
            |
            |  final class Response200ApplicationJson implements Response200<TodoDto> {
            |    private final int status;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<TodoDto> content;
            |
            |    public Response200ApplicationJson(int status, java.util.Map<String, java.util.List<Object>> headers, Wirespec.Content<TodoDto> content) {
            |      this.status = status;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public Response200ApplicationJson(
            |      TodoDto body
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
            |    public Wirespec.Content<TodoDto> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  final class Response500ApplicationJson implements Response500<Error> {
            |    private final int status;
            |    private final java.util.Map<String, java.util.List<Object>> headers;
            |    private final Wirespec.Content<Error> content;
            |
            |    public Response500ApplicationJson(int status, java.util.Map<String, java.util.List<Object>> headers, Wirespec.Content<Error> content) {
            |      this.status = status;
            |      this.headers = headers;
            |      this.content = content;
            |    }
            |
            |    public Response500ApplicationJson(
            |      Error body
            |    ) {
            |      this.status = 500;
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
            |    public Wirespec.Content<Error> getContent() {
            |      return content;
            |    }
            |  }
            |
            |  static <B, Req extends Request<?>> Function<Wirespec.Request<B>, Req> REQUEST_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return request -> {
            |      if (request.getContent().type().equals("application/json")) {
            |        Wirespec.Content<PotentialTodoDto> content = contentMapper.read(request.getContent(), Wirespec.getType(PotentialTodoDto.class, false));
            |        return (Req) new RequestApplicationJson(request.getPath(), request.getMethod(), request.getQuery(), request.getHeaders(), content);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    };
            |  }
            |  static <B, Res extends Response<?>> Function<Wirespec.Response<B>, Res> RESPONSE_MAPPER(Wirespec.ContentMapper<B> contentMapper) {
            |    return response -> {
            |      if (response.getStatus() == 200 && response.getContent().type().equals("application/json")) {
            |        Wirespec.Content<TodoDto> content = contentMapper.read(response.getContent(), Wirespec.getType(TodoDto.class, false));
            |        return (Res) new Response200ApplicationJson(response.getStatus(), response.getHeaders(), content);
            |      }
            |      if (response.getStatus() == 500 && response.getContent().type().equals("application/json")) {
            |        Wirespec.Content<Error> content = contentMapper.read(response.getContent(), Wirespec.getType(Error.class, false));
            |        return (Res) new Response500ApplicationJson(response.getStatus(), response.getHeaders(), content);
            |      }
            |      throw new IllegalStateException("Unknown response type");
            |    };
            |  }
            |
            |  public CompletableFuture<Response<?>> putTodo(Request<?> request);
            |
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
            |case class PotentialTodoDto(
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |case class Token(
            |  val iss: String
            |)
            |
            |case class TodoDto(
            |  val id: String,
            |  val name: String,
            |  val done: Boolean
            |)
            |
            |case class Error(
            |  val code: Long,
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
            |  export type Content<T> = { type:string, body:T }
            |  export type Request<T> = { path:string, method: Method, query?: Record<string, any[]>, headers?: Record<string, any[]>, content?:Content<T> }
            |  export type Response<T> = { status:number, headers?: Record<string, any[]>, content?:Content<T> }
            |}
            |export module PutTodo {
            |  export const PATH = "/todos/:id"
            |  export const METHOD = "PUT"
            |  type RequestApplicationJson = { path: `/todos/${'$'}{string}`, method: "PUT", headers: {  "token": Token}, query: {  "done": boolean}, content: { type: "application/json", body: PotentialTodoDto } } 
            |  export type Request = RequestApplicationJson
            |  type Response200ApplicationJson = { status: 200, content: { type: "application/json", body: TodoDto } }
            |  type Response500ApplicationJson = { status: 500, content: { type: "application/json", body: Error } }
            |  export type Response = Response200ApplicationJson | Response500ApplicationJson
            |  export type Handler = (request:Request) => Promise<Response>
            |  export type Call = {
            |    putTodo: Handler
            |  }
            |  export const call = (handler:Handler) => ({METHOD, PATH, handler})
            |  export const requestApplicationJson = (props:{  "id": string,  "done": boolean,  "token": Token,  "body": PotentialTodoDto}) => ({path: `/todos/${'$'}{props.id}`, method: "PUT", query: {"done": props.done}, headers: {"token": props.token}, content: {type: "application/json", body: props.body}} as const)
            |  export const response200ApplicationJson = (props:{  "body": TodoDto}) => ({status: 200, headers: {}, content: {type: "application/json", body: props.body}} as const)
            |  export const response500ApplicationJson = (props:{  "body": Error}) => ({status: 500, headers: {}, content: {type: "application/json", body: props.body}} as const)
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
