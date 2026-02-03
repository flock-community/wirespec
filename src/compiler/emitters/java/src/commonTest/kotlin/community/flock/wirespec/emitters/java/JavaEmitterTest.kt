package community.flock.wirespec.emitters.java

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
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
            |}
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
            |public record TodoWithoutProperties () {
            |}
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
            |public record UUID (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value;
            |  }
            |  public static Boolean validate(UUID record) {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
            |  }
            |  @Override
            |  public String getValue() {
            |    return value;
            |  }
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
            |  public static record Path (
            |    String id
            |  ) implements Wirespec.Path {
            |  }
            |  public static record Queries (
            |    Boolean done,
            |    java.util.Optional<String> name
            |  ) implements Wirespec.Queries {
            |  }
            |  public static record RequestHeaders (
            |    Token token,
            |    java.util.Optional<Token> RefreshToken
            |  ) implements Wirespec.Request.Headers {
            |  }
            |  public static record Request (
            |    Path path,
            |    Wirespec.Method method,
            |    Queries queries,
            |    RequestHeaders headers,
            |    PotentialTodoDto body
            |  ) implements Wirespec.Request<PotentialTodoDto> {
            |    public Request(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> RefreshToken, PotentialTodoDto body) {
            |      this(new Path(id), Wirespec.Method.PUT, new Queries(
            |        done,
            |        name
            |      ), new RequestHeaders(
            |        token,
            |        RefreshToken
            |      ), body);
            |    }
            |  }
            |  public sealed interface Response<T> extends Wirespec.Response<T> {
            |  }
            |  public sealed interface Response2XX<T> extends Response<T> {
            |  }
            |  public sealed interface Response5XX<T> extends Response<T> {
            |  }
            |  public sealed interface ResponseTodoDto extends Response<TodoDto> {
            |  }
            |  public sealed interface ResponseError extends Response<Error> {
            |  }
            |  public static record Response200 (
            |    Integer status,
            |    Headers headers,
            |    TodoDto body
            |  ) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    public Response200(TodoDto body) {
            |      this(200, new Headers(), body);
            |    }
            |    public static record Headers () implements Wirespec.Response.Headers {
            |    }
            |  }
            |  public static record Response201 (
            |    Integer status,
            |    Headers headers,
            |    TodoDto body
            |  ) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    public Response201(Token token, java.util.Optional<Token> refreshToken, TodoDto body) {
            |      this(201, new Headers(
            |        token,
            |        refreshToken
            |      ), body);
            |    }
            |    public static record Headers (
            |      Token token,
            |      java.util.Optional<Token> refreshToken
            |    ) implements Wirespec.Response.Headers {
            |    }
            |  }
            |  public static record Response500 (
            |    Integer status,
            |    Headers headers,
            |    Error body
            |  ) implements Response5XX<Error>, ResponseError {
            |    public Response500(Error body) {
            |      this(500, new Headers(), body);
            |    }
            |    public static record Headers () implements Wirespec.Response.Headers {
            |    }
            |  }
            |  public interface Handler extends Wirespec.Handler {
            |    static public Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method().name(),
            |        java.util.List.of("todos", serialization.serializePath(request.path().id(), Wirespec.getType(String.class, null))),
            |        java.util.Map.ofEntries(java.util.Map.entry("done", serialization.serializeParam(request.queries().done(), Wirespec.getType(Boolean.class, null))), java.util.Map.entry("name", serialization.serializeParam(request.queries().name(), Wirespec.getType(String.class, java.util.Optional.class)))),
            |        java.util.Map.ofEntries(java.util.Map.entry("token", serialization.serializeParam(request.headers().token(), Wirespec.getType(Token.class, null))), java.util.Map.entry("Refresh-Token", serialization.serializeParam(request.headers().RefreshToken(), Wirespec.getType(Token.class, java.util.Optional.class)))),
            |        serialization.serializeBody(request.body(), Wirespec.getType(PotentialTodoDto.class, null))
            |      );
            |    }
            |    static public Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
            |      return new Request(
            |        serialization.deserializePath(request.path().get(1), Wirespec.getType(String.class, null)),
            |        serialization.deserializeParam(request.queries().getOrDefault("done", java.util.Collections.emptyList()), Wirespec.getType(Boolean.class, null)),
            |        serialization.deserializeParam(request.queries().getOrDefault("name", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
            |        serialization.deserializeParam(request.headers().getOrDefault("token", java.util.Collections.emptyList()), Wirespec.getType(Token.class, null)),
            |        serialization.deserializeParam(request.headers().getOrDefault("Refresh-Token", java.util.Collections.emptyList()), Wirespec.getType(Token.class, java.util.Optional.class)),
            |        serialization.deserializeBody(request.body(), Wirespec.getType(PotentialTodoDto.class, null))
            |      );
            |    }
            |    static public Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
            |      if (response instanceof Response200 r) {
            |        return new Wirespec.RawResponse(
            |          r.status(),
            |          java.util.Collections.emptyMap(),
            |          serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, null))
            |        );
            |      } else if (response instanceof Response201 r) {
            |        return new Wirespec.RawResponse(
            |          r.status(),
            |          java.util.Map.ofEntries(java.util.Map.entry("token", serialization.serializeParam(r.headers().token(), Wirespec.getType(Token.class, null))), java.util.Map.entry("refreshToken", serialization.serializeParam(r.headers().refreshToken(), Wirespec.getType(Token.class, java.util.Optional.class)))),
            |          serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, null))
            |        );
            |      } else if (response instanceof Response500 r) {
            |        return new Wirespec.RawResponse(
            |          r.status(),
            |          java.util.Collections.emptyMap(),
            |          serialization.serializeBody(r.body(), Wirespec.getType(Error.class, null))
            |        );
            |      } else {
            |        throw new IllegalStateException(("Cannot match response with status: " + response.status()));
            |      }
            |    }
            |    static public Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
            |      switch (response.statusCode()) {
            |          case 200 -> {
            |            return new Response200(serialization.deserializeBody(response.body(), Wirespec.getType(TodoDto.class, null)));
            |          }
            |          case 201 -> {
            |            return new Response201(
            |              serialization.deserializeParam<Token>(response.headers().getOrDefault("token", java.util.Collections.emptyList()), Wirespec.getType(Token.class, null)),
            |              serialization.deserializeParam<java.util.Optional<Token>>(response.headers().getOrDefault("refreshToken", java.util.Collections.emptyList()), Wirespec.getType(Token.class, java.util.Optional.class)),
            |              serialization.deserializeBody(response.body(), Wirespec.getType(TodoDto.class, null))
            |            );
            |          }
            |          case 500 -> {
            |            return new Response500(serialization.deserializeBody(response.body(), Wirespec.getType(Error.class, null)));
            |          }
            |          default -> {
            |            throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
            |          }
            |      }
            |    }
            |    public java.util.concurrent.CompletableFuture<Response<?>> putTodo(Request request);
            |    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override
            |      public String getPathTemplate() {
            |        return "/todos/{id}";
            |      }
            |      @Override
            |      public String getMethod() {
            |        return "PUT";
            |      }
            |      @Override
            |      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
            |        return new Wirespec.ServerEdge<>() {
            |        @Override public Request from(Wirespec.RawRequest request) {
            |          return fromRequest(serialization, request);
            |        }
            |        @Override public Wirespec.RawResponse to(Response<?> response) {
            |          return toResponse(serialization, response);
            |        }};
            |      }
            |      @Override
            |      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
            |        return new Wirespec.ClientEdge<>() {
            |        @Override public Wirespec.RawRequest to(Request request) {
            |          return toRequest(serialization, request);
            |        }
            |        @Override public Response<?> from(Wirespec.RawResponse response) {
            |          return fromResponse(serialization, response);
            |        }};
            |      }
            |    }
            |  }
            |}
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record PotentialTodoDto (
            |  String name,
            |  Boolean done
            |) {
            |}
            |
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record Token (
            |  String iss
            |) {
            |}
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
            |}
            |
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record Error (
            |  Long code,
            |  String description
            |) {
            |}
            |
        """.trimMargin()
        CompileFullEndpointTest.compiler { JavaEmitter() } shouldBeRight java
    }

    @Test
    fun compileChannelTest() {
        val java = """
            |package community.flock.wirespec.generated.channel;
            |
            |@FunctionalInterface
            |public interface Queue extends Wirespec.Channel {
            |  public void invoke(String message);
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
            |  public static record Path () implements Wirespec.Path {
            |  }
            |  public static record Queries () implements Wirespec.Queries {
            |  }
            |  public static record RequestHeaders () implements Wirespec.Request.Headers {
            |  }
            |  public static record Request (
            |    Path path,
            |    Wirespec.Method method,
            |    Queries queries,
            |    RequestHeaders headers,
            |    Void body
            |  ) implements Wirespec.Request<Void> {
            |    public Request() {
            |      this(new Path(), Wirespec.Method.GET, new Queries(), new RequestHeaders(), null);
            |    }
            |  }
            |  public sealed interface Response<T> extends Wirespec.Response<T> {
            |  }
            |  public sealed interface Response2XX<T> extends Response<T> {
            |  }
            |  public sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> {
            |  }
            |  public static record Response200 (
            |    Integer status,
            |    Headers headers,
            |    java.util.List<TodoDto> body
            |  ) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
            |    public Response200(java.util.List<TodoDto> body) {
            |      this(200, new Headers(), body);
            |    }
            |    public static record Headers () implements Wirespec.Response.Headers {
            |    }
            |  }
            |  public interface Handler extends Wirespec.Handler {
            |    static public Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method().name(),
            |        java.util.List.of("todos"),
            |        java.util.Collections.emptyMap(),
            |        java.util.Collections.emptyMap(),
            |        null
            |      );
            |    }
            |    static public Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
            |      return new Request();
            |    }
            |    static public Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
            |      if (response instanceof Response200 r) {
            |        return new Wirespec.RawResponse(
            |          r.status(),
            |          java.util.Collections.emptyMap(),
            |          serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, java.util.List.class))
            |        );
            |      } else {
            |        throw new IllegalStateException(("Cannot match response with status: " + response.status()));
            |      }
            |    }
            |    static public Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
            |      switch (response.statusCode()) {
            |          case 200 -> {
            |            return new Response200(serialization.deserializeBody(response.body(), Wirespec.getType(TodoDto.class, java.util.List.class)));
            |          }
            |          default -> {
            |            throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
            |          }
            |      }
            |    }
            |    public java.util.concurrent.CompletableFuture<Response<?>> getTodos(Request request);
            |    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override
            |      public String getPathTemplate() {
            |        return "/todos";
            |      }
            |      @Override
            |      public String getMethod() {
            |        return "GET";
            |      }
            |      @Override
            |      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
            |        return new Wirespec.ServerEdge<>() {
            |        @Override public Request from(Wirespec.RawRequest request) {
            |          return fromRequest(serialization, request);
            |        }
            |        @Override public Wirespec.RawResponse to(Response<?> response) {
            |          return toResponse(serialization, response);
            |        }};
            |      }
            |      @Override
            |      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
            |        return new Wirespec.ClientEdge<>() {
            |        @Override public Wirespec.RawRequest to(Request request) {
            |          return toRequest(serialization, request);
            |        }
            |        @Override public Response<?> from(Wirespec.RawResponse response) {
            |          return fromResponse(serialization, response);
            |        }};
            |      }
            |    }
            |  }
            |}
            |package community.flock.wirespec.generated.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoDto (
            |  String description
            |) {
            |}
            |
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
            |public record TodoId (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value;
            |  }
            |  public static Boolean validate(TodoId record) {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(record.value).find();
            |  }
            |  @Override
            |  public String getValue() {
            |    return value;
            |  }
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
            |}
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record UserAccountToken (
            |  String token
            |) implements UserAccount {
            |}
            |
            |package community.flock.wirespec.generated.model;
            |
            |public record User (
            |  String username,
            |  UserAccount account
            |) {
            |}
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
            |}
            |
        """.trimMargin()

        CompileTypeTest.compiler { JavaEmitter() } shouldBeRight java
    }

    private fun EmitContext.emitFirst(node: Definition) = emitters.map {
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        it.emit(ast, logger).first().result
    }
}
