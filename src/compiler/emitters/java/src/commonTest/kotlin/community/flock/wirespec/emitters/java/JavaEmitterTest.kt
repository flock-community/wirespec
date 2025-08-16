package community.flock.wirespec.emitters.java

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.parse.Definition
import community.flock.wirespec.compiler.core.parse.Module
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JavaEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(JavaEmitter())
    }

    @Test
    fun testEmitterType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |public record Todo (
            |  String name,
            |  java.util.Optional<String> description,
            |  java.util.List<String> notes,
            |  Boolean done
            |) {
            |};
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.type)
        res shouldBe expected
    }

    @Test
    fun testEmitterEmptyType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |public record TodoWithoutProperties (
            |
            |) {
            |};
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.emptyType)
        res shouldBe expected
    }

    @Test
    fun testEmitterRefined() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record UUID (String value) implements Wirespec.Refined {
            |  @Override
            |  public String toString() { return value; }
            |  public static boolean validate(UUID record) {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
            |  }
            |  @Override
            |  public String getValue() { return value; }
            |}
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.refined)
        res shouldBe expected
    }

    @Test
    fun testEmitterEnum() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public enum TodoStatus implements Wirespec.Enum {
            |  OPEN("OPEN"),
            |  IN_PROGRESS("IN_PROGRESS"),
            |  CLOSE("CLOSE");
            |  public final String label;
            |  TodoStatus(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |  @Override
            |  public String getLabel() {
            |    return label;
            |  }
            |}
            |
            """.trimMargin(),
        )

        val res = emitContext.emitFirst(NodeFixtures.enum)
        res shouldBe expected
    }

    @Test
    fun compileFullEndpointTest() {
        val java = """
            |package community.flock.wirespec.generated.endpoint;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import community.flock.wirespec.generated.model.Token;
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
            |    Boolean done,
            |    java.util.Optional<String> name
            |  ) implements Wirespec.Queries {}
            |
            |  public record RequestHeaders(
            |    Token token,
            |    java.util.Optional<Token> refreshToken
            |  ) implements Wirespec.Request.Headers {}
            |
            |  class Request implements Wirespec.Request<PotentialTodoDto> {
            |    private final Path path;
            |    private final Wirespec.Method method;
            |    private final Queries queries;
            |    private final RequestHeaders headers;
            |    private final PotentialTodoDto body;
            |    public Request(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> refreshToken, PotentialTodoDto body) {
            |      this.path = new Path(id);
            |      this.method = Wirespec.Method.PUT;
            |      this.queries = new Queries(done, name);
            |      this.headers = new RequestHeaders(token, refreshToken);
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
            |  record Response201(Token token, java.util.Optional<Token> refreshToken, TodoDto body) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    @Override public int getStatus() { return 201; }
            |    @Override public Headers getHeaders() { return new Headers(token, refreshToken); }
            |    @Override public TodoDto getBody() { return body; }
            |    public record Headers(
            |    Token token,
            |    java.util.Optional<Token> refreshToken
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
            |        java.util.List.of("todos", serialization.serialize(request.path.id, Wirespec.getType(String.class, null))),
            |        java.util.Map.ofEntries(java.util.Map.entry("done", serialization.serializeParam(request.queries.done, Wirespec.getType(Boolean.class, null))), java.util.Map.entry("name", serialization.serializeParam(request.queries.name, Wirespec.getType(String.class, java.util.Optional.class)))),
            |        java.util.Map.ofEntries(java.util.Map.entry("token", serialization.serializeParam(request.headers.token, Wirespec.getType(Token.class, null))), java.util.Map.entry("refreshToken", serialization.serializeParam(request.headers.refreshToken, Wirespec.getType(Token.class, java.util.Optional.class)))),
            |        serialization.serialize(request.getBody(), Wirespec.getType(PotentialTodoDto.class, null))
            |      );
            |    }
            |
            |    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
            |      return new Request(
            |        serialization.deserialize(request.path().get(1), Wirespec.getType(String.class, null)),
            |        serialization.deserializeParam(request.queries().getOrDefault("done", java.util.Collections.emptyList()), Wirespec.getType(Boolean.class, null)),
            |        serialization.deserializeParam(request.queries().getOrDefault("name", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
            |        serialization.deserializeParam(request.headers().getOrDefault("token", java.util.Collections.emptyList()), Wirespec.getType(Token.class, null)),
            |        serialization.deserializeParam(request.headers().getOrDefault("refreshToken", java.util.Collections.emptyList()), Wirespec.getType(Token.class, java.util.Optional.class)),
            |        serialization.deserialize(request.body(), Wirespec.getType(PotentialTodoDto.class, null))
            |      );
            |    }
            |
            |    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
            |      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, null))); }
            |      if (response instanceof Response201 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Map.ofEntries(java.util.Map.entry("token", serialization.serializeParam(r.getHeaders().token(), Wirespec.getType(Token.class, null))), java.util.Map.entry("refreshToken", serialization.serializeParam(r.getHeaders().refreshToken(), Wirespec.getType(Token.class, java.util.Optional.class)))), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, null))); }
            |      if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(Error.class, null))); }
            |      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
            |    }
            |
            |    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
            |      switch (response.statusCode()) {
            |        case 200: return new Response200(
            |        serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, null))
            |      );
            |        case 201: return new Response201(
            |        serialization.deserializeParam(response.headers().getOrDefault("token", java.util.Collections.emptyList()), Wirespec.getType(Token.class, null)),
            |        serialization.deserializeParam(response.headers().getOrDefault("refreshToken", java.util.Collections.emptyList()), Wirespec.getType(Token.class, java.util.Optional.class)),
            |        serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, null))
            |      );
            |        case 500: return new Response500(
            |        serialization.deserialize(response.body(), Wirespec.getType(Error.class, null))
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
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import community.flock.wirespec.generated.endpoint.PutTodo;
            |
            |import community.flock.wirespec.generated.model.Token;
            |import community.flock.wirespec.generated.model.PotentialTodoDto;
            |import community.flock.wirespec.generated.model.TodoDto;
            |import community.flock.wirespec.generated.model.Error;   
            |
            |public class Client {
            |  private final Wirespec.Serialization<String> serialization;
            |  private final java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler;
            |
            |  public Client(Wirespec.Serialization<String> serialization, java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler) {
            |    this.serialization = serialization;
            |    this.handler = handler;
            |  }
            |
            |  public java.util.concurrent.CompletableFuture<PutTodo.Response<?>> putTodo(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> refreshToken, PotentialTodoDto body) {
            |    var req = new PutTodo.Request(id, done, name, token, refreshToken, body);
            |    var rawReq = PutTodo.Handler.toRequest(serialization, req);
            |    return handler.apply(rawReq)
            |     .thenApply(rawRes -> PutTodo.Handler.fromResponse(serialization, rawRes));
            |  }
            |}
        """.trimMargin()
        CompileFullEndpointTest.compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun compileChannelTest() {
        val java = """
            |package community.flock.wirespec.generated.channel;
            |
            |
            |
            |public interface Queue {
            |   void invoke(String message);
            |}
            |
        """.trimMargin()

        CompileChannelTest.compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun compileEnumTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public enum MyAwesomeEnum implements Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE"),
            |  UnitedKingdom("UnitedKingdom");
            |  public final String label;
            |  MyAwesomeEnum(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |  @Override
            |  public String getLabel() {
            |    return label;
            |  }
            |}
            |
        """.trimMargin()

        CompileEnumTest.compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun compileMinimalEndpointTest() {
        val java = """
            |package community.flock.wirespec.generated.endpoint;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import community.flock.wirespec.generated.model.TodoDto;
            |
            |public interface GetTodos extends Wirespec.Endpoint {
            |  class Path implements Wirespec.Path {}
            |
            |  class Queries implements Wirespec.Queries {}
            |
            |  class RequestHeaders implements Wirespec.Request.Headers {}
            |
            |  class Request implements Wirespec.Request<Void> {
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
            |  sealed interface Response<T> extends Wirespec.Response<T> {}
            |  sealed interface Response2XX<T> extends Response<T> {}
            |  sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> {}
            |
            |  record Response200(java.util.List<TodoDto> body) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
            |    @Override public int getStatus() { return 200; }
            |    @Override public Headers getHeaders() { return new Headers(); }
            |    @Override public java.util.List<TodoDto> getBody() { return body; }
            |    class Headers implements Wirespec.Response.Headers {}
            |  }
            |
            |  interface Handler extends Wirespec.Handler {
            |
            |    static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method.name(),
            |        java.util.List.of("todos"),
            |        java.util.Collections.emptyMap(),
            |        java.util.Collections.emptyMap(),
            |        serialization.serialize(request.getBody(), null)
            |      );
            |    }
            |
            |    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
            |      return new Request();
            |    }
            |
            |    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
            |      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, java.util.List.class))); }
            |      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
            |    }
            |
            |    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
            |      switch (response.statusCode()) {
            |        case 200: return new Response200(
            |        serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, java.util.List.class))
            |      );
            |        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
            |      }
            |    }
            |
            |    java.util.concurrent.CompletableFuture<Response<?>> getTodos(Request request);
            |    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override public String getPathTemplate() { return "/todos"; }
            |      @Override public String getMethod() { return "GET"; }
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
            |public record TodoDto (
            |  String description
            |) {
            |};
            |
            |package community.flock.wirespec.generated;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import community.flock.wirespec.generated.endpoint.GetTodos;
            |
            |import community.flock.wirespec.generated.model.TodoDto;   
            |
            |public class Client {
            |  private final Wirespec.Serialization<String> serialization;
            |  private final java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler;
            |
            |  public Client(Wirespec.Serialization<String> serialization, java.util.function.Function<Wirespec.RawRequest, java.util.concurrent.CompletableFuture<Wirespec.RawResponse>> handler) {
            |    this.serialization = serialization;
            |    this.handler = handler;
            |  }
            |
            |  public java.util.concurrent.CompletableFuture<GetTodos.Response<?>> getTodos() {
            |    var req = new GetTodos.Request();
            |    var rawReq = GetTodos.Handler.toRequest(serialization, req);
            |    return handler.apply(rawReq)
            |     .thenApply(rawRes -> GetTodos.Handler.fromResponse(serialization, rawRes));
            |  }
            |}
        """.trimMargin()
        CompileMinimalEndpointTest.compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun compileRefinedTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoId (String value) implements Wirespec.Refined {
            |  @Override
            |  public String toString() { return value; }
            |  public static boolean validate(TodoId record) {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
            |  }
            |  @Override
            |  public String getValue() { return value; }
            |}
            |
        """.trimMargin()

        CompileRefinedTest.compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun compileUnionTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |
            |public sealed interface UserAccount permits UserAccountPassword, UserAccountToken {}
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record UserAccountPassword (
            |  String username,
            |  String password
            |) implements UserAccount {
            |};
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record UserAccountToken (
            |  String token
            |) implements UserAccount {
            |};
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record User (
            |  String username,
            |  UserAccount account
            |) {
            |};
            |
        """.trimMargin()

        CompileUnionTest.compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun compileTypeTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |
            |public record Request (
            |  String type,
            |  String url,
            |  java.util.Optional<String> BODY_TYPE,
            |  java.util.List<String> params,
            |  java.util.Map<String, String> headers,
            |  java.util.Optional<java.util.Map<String, java.util.Optional<java.util.List<java.util.Optional<String>>>>> body
            |) {
            |};
            |
        """.trimMargin()

        CompileTypeTest.compiler { JavaEmitter() } shouldBeRight java
    }

    private fun EmitContext.emitFirst(node: Definition) = emitters.map { it.emit(Module("", nonEmptyListOf(node)), logger).first().result }
}
