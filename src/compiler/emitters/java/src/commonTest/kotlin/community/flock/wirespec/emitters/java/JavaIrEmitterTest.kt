package community.flock.wirespec.emitters.java

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileComplexModelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileNestedTypeTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.test.NodeFixtures
import community.flock.wirespec.compiler.utils.NoLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JavaIrEmitterTest {

    private val emitContext = object : EmitContext, NoLogger {
        override val emitters = nonEmptySetOf(JavaIrEmitter())
    }

    @Test
    fun testEmitterType() {
        val expected = listOf(
            """
            |package community.flock.wirespec.generated.model;
            |public record Todo (
            |  String name,
            |  java.util.Optional<String> description,
            |  java.util.List<String> notes,
            |  Boolean done
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
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
            |public record TodoWithoutProperties () implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
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
            |import community.flock.wirespec.java.Wirespec;
            |public record UUID (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String value() {
            |    return value;
            |  }
            |};
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
            |import community.flock.wirespec.java.Wirespec;
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
            |  public String label() {
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
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Token;
            |import community.flock.wirespec.generated.model.Token;
            |import community.flock.wirespec.generated.model.PotentialTodoDto;
            |import community.flock.wirespec.generated.model.TodoDto;
            |import community.flock.wirespec.generated.model.Error;
            |public interface PutTodo extends Wirespec.Endpoint {
            |  public static record Path (
            |    String id
            |  ) implements Wirespec.Path {
            |  };
            |  public static record Queries (
            |    Boolean done,
            |    java.util.Optional<String> name
            |  ) implements Wirespec.Queries {
            |  };
            |  public static record RequestHeaders (
            |    Token token,
            |    java.util.Optional<Token> refreshToken
            |  ) implements Wirespec.Request.Headers {
            |  };
            |  public static record Request (
            |    Path path,
            |    Wirespec.Method method,
            |    Queries queries,
            |    RequestHeaders headers,
            |    PotentialTodoDto body
            |  ) implements Wirespec.Request<PotentialTodoDto> {
            |    public Request(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> refreshToken, PotentialTodoDto body) {
            |      this(new Path(id), Wirespec.Method.PUT, new Queries(
            |        done,
            |        name
            |      ), new RequestHeaders(
            |        token,
            |        refreshToken
            |      ), body);
            |    }
            |  };
            |  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response5XX, ResponseTodoDto, ResponseError {}
            |  public sealed interface Response2XX<T> extends Response<T> permits Response200, Response201 {}
            |  public sealed interface Response5XX<T> extends Response<T> permits Response500 {}
            |  public sealed interface ResponseTodoDto extends Response<TodoDto> permits Response200, Response201 {}
            |  public sealed interface ResponseError extends Response<Error> permits Response500 {}
            |  public static record Response200 (
            |    Integer status,
            |    Headers headers,
            |    TodoDto body
            |  ) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    public Response200(TodoDto body) {
            |      this(200, new Headers(), body);
            |    }
            |    public static record Headers () implements Wirespec.Response.Headers {
            |    };
            |  };
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
            |    };
            |  };
            |  public static record Response500 (
            |    Integer status,
            |    Headers headers,
            |    Error body
            |  ) implements Response5XX<Error>, ResponseError {
            |    public Response500(Error body) {
            |      this(500, new Headers(), body);
            |    }
            |    public static record Headers () implements Wirespec.Response.Headers {
            |    };
            |  };
            |  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
            |    return new Wirespec.RawRequest(
            |      request.method().name(),
            |      java.util.List.of("todos", serialization.<String>serializePath(request.path().id(), Wirespec.getType(String.class, null))),
            |      java.util.Map.ofEntries(java.util.Map.entry("done", serialization.<Boolean>serializeParam(request.queries().done(), Wirespec.getType(Boolean.class, null))), java.util.Map.entry("name", request.queries().name().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of()))),
            |      java.util.Map.ofEntries(java.util.Map.entry("token", serialization.<Token>serializeParam(request.headers().token(), Wirespec.getType(Token.class, null))), java.util.Map.entry("Refresh-Token", request.headers().refreshToken().map(it -> serialization.<Token>serializeParam(it, Wirespec.getType(Token.class, null))).orElse(java.util.List.<String>of()))),
            |      java.util.Optional.of(serialization.<PotentialTodoDto>serializeBody(request.body(), Wirespec.getType(PotentialTodoDto.class, null)))
            |    );
            |  }
            |  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
            |    return new Request(
            |      serialization.<String>deserializePath(request.path().get(1), Wirespec.getType(String.class, null)),
            |      java.util.Optional.ofNullable(request.queries().get("done")).map(it -> serialization.<Boolean>deserializeParam(it, Wirespec.getType(Boolean.class, null))).orElseThrow(() -> new IllegalStateException("Param done cannot be null")),
            |      java.util.Optional.ofNullable(request.queries().get("name")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
            |      java.util.Optional.ofNullable(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("token")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<Token>deserializeParam(it, Wirespec.getType(Token.class, null))).orElseThrow(() -> new IllegalStateException("Param token cannot be null")),
            |      java.util.Optional.ofNullable(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Refresh-Token")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<Token>deserializeParam(it, Wirespec.getType(Token.class, null))),
            |      request.body().map(it -> serialization.<PotentialTodoDto>deserializeBody(it, Wirespec.getType(PotentialTodoDto.class, null))).orElseThrow(() -> new IllegalStateException("body is null"))
            |    );
            |  }
            |  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
            |    if (response instanceof Response200 r) {
            |      return new Wirespec.RawResponse(
            |        r.status(),
            |        java.util.Collections.emptyMap(),
            |        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, null)))
            |      );
            |    } else if (response instanceof Response201 r) {
            |      return new Wirespec.RawResponse(
            |        r.status(),
            |        java.util.Map.ofEntries(java.util.Map.entry("token", serialization.<Token>serializeParam(r.headers().token(), Wirespec.getType(Token.class, null))), java.util.Map.entry("refreshToken", r.headers().refreshToken().map(it -> serialization.<Token>serializeParam(it, Wirespec.getType(Token.class, null))).orElse(java.util.List.<String>of()))),
            |        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, null)))
            |      );
            |    } else if (response instanceof Response500 r) {
            |      return new Wirespec.RawResponse(
            |        r.status(),
            |        java.util.Collections.emptyMap(),
            |        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(Error.class, null)))
            |      );
            |    } else {
            |      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
            |    }
            |  }
            |  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
            |    switch (response.statusCode()) {
            |        case 200 -> {
            |          return new Response200(response.body().map(it -> serialization.<TodoDto>deserializeBody(it, Wirespec.getType(TodoDto.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
            |        }
            |        case 201 -> {
            |          return new Response201(
            |            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("token")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<Token>deserializeParam(it, Wirespec.getType(Token.class, null))).orElseThrow(() -> new IllegalStateException("Param token cannot be null")),
            |            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("refreshToken")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<Token>deserializeParam(it, Wirespec.getType(Token.class, null))),
            |            response.body().map(it -> serialization.<TodoDto>deserializeBody(it, Wirespec.getType(TodoDto.class, null))).orElseThrow(() -> new IllegalStateException("body is null"))
            |          );
            |        }
            |        case 500 -> {
            |          return new Response500(response.body().map(it -> serialization.<Error>deserializeBody(it, Wirespec.getType(Error.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
            |        }
            |        default -> {
            |          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
            |        }
            |    }
            |  }
            |  public interface Handler extends Wirespec.Handler {
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
            |            return fromRawRequest(serialization, request);
            |        }
            |        @Override public Wirespec.RawResponse to(Response<?> response) {
            |            return toRawResponse(serialization, response);
            |        }
            |        };
            |      }
            |      @Override
            |      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
            |        return new Wirespec.ClientEdge<>() {
            |        @Override public Wirespec.RawRequest to(Request request) {
            |            return toRawRequest(serialization, request);
            |        }
            |        @Override public Response<?> from(Wirespec.RawResponse response) {
            |            return fromRawResponse(serialization, response);
            |        }
            |        };
            |      }
            |    };
            |  }
            |  public interface Call extends Wirespec.Call {
            |    public java.util.concurrent.CompletableFuture<Response<?>> putTodo(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> refreshToken, PotentialTodoDto body);
            |  }
            |}
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record PotentialTodoDto (
            |  String name,
            |  Boolean done
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Token (
            |  String iss
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TodoDto (
            |  String id,
            |  String name,
            |  Boolean done
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Error (
            |  Long code,
            |  String description
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.client;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Token;
            |import community.flock.wirespec.generated.model.Token;
            |import community.flock.wirespec.generated.model.PotentialTodoDto;
            |import community.flock.wirespec.generated.model.TodoDto;
            |import community.flock.wirespec.generated.model.Error;
            |import community.flock.wirespec.generated.endpoint.PutTodo;
            |public record PutTodoClient (
            |  Wirespec.Serialization serialization,
            |  Wirespec.Transportation transportation
            |) implements PutTodo.Call {
            |  @Override
            |  public java.util.concurrent.CompletableFuture<PutTodo.Response<?>> putTodo(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> refreshToken, PotentialTodoDto body) {
            |    final var request = new PutTodo.Request(
            |      id,
            |      done,
            |      name,
            |      token,
            |      refreshToken,
            |      body
            |    );
            |    final var rawRequest = PutTodo.toRawRequest(serialization(), request);
            |    return transportation().transport(rawRequest).thenApply(rawResponse -> PutTodo.fromRawResponse(serialization(), rawResponse));
            |  }
            |};
            |
            |package community.flock.wirespec.generated;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Token;
            |import community.flock.wirespec.generated.model.PotentialTodoDto;
            |import community.flock.wirespec.generated.model.TodoDto;
            |import community.flock.wirespec.generated.model.Error;
            |import community.flock.wirespec.generated.endpoint.PutTodo;
            |import community.flock.wirespec.generated.client.PutTodoClient;
            |public record Client (
            |  Wirespec.Serialization serialization,
            |  Wirespec.Transportation transportation
            |) implements PutTodo.Call {
            |  @Override
            |  public java.util.concurrent.CompletableFuture<PutTodo.Response<?>> putTodo(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> refreshToken, PotentialTodoDto body) {
            |    return new PutTodoClient(
            |      serialization(),
            |      transportation()
            |    ).putTodo(id, done, name, token, refreshToken, body);
            |  }
            |};
            |
        """.trimMargin()

        CompileFullEndpointTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileChannelTest() {
        val java = """
            |package community.flock.wirespec.generated.channel;
            |@FunctionalInterface
            |public interface Queue extends Wirespec.Channel {
            |  public void invoke(String message);
            |}
            |
        """.trimMargin()

        CompileChannelTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileEnumTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public enum MyAwesomeEnum implements Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE"),
            |  UnitedKingdom("UnitedKingdom"),
            |  _1("-1"),
            |  _0("0"),
            |  _10("10"),
            |  _999("-999"),
            |  _88("88");
            |  public final String label;
            |  MyAwesomeEnum(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |  public String label() {
            |    return label;
            |  }
            |}
            |
        """.trimMargin()

        CompileEnumTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileMinimalEndpointTest() {
        val java = """
            |package community.flock.wirespec.generated.endpoint;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TodoDto;
            |public interface GetTodos extends Wirespec.Endpoint {
            |  public static record Path () implements Wirespec.Path {
            |  };
            |  public static record Queries () implements Wirespec.Queries {
            |  };
            |  public static record RequestHeaders () implements Wirespec.Request.Headers {
            |  };
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
            |  };
            |  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, ResponseListTodoDto {}
            |  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
            |  public sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> permits Response200 {}
            |  public static record Response200 (
            |    Integer status,
            |    Headers headers,
            |    java.util.List<TodoDto> body
            |  ) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
            |    public Response200(java.util.List<TodoDto> body) {
            |      this(200, new Headers(), body);
            |    }
            |    public static record Headers () implements Wirespec.Response.Headers {
            |    };
            |  };
            |  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
            |    return new Wirespec.RawRequest(
            |      request.method().name(),
            |      java.util.List.of("todos"),
            |      java.util.Collections.emptyMap(),
            |      java.util.Collections.emptyMap(),
            |      java.util.Optional.empty()
            |    );
            |  }
            |  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
            |    return new Request();
            |  }
            |  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
            |    if (response instanceof Response200 r) {
            |      return new Wirespec.RawResponse(
            |        r.status(),
            |        java.util.Collections.emptyMap(),
            |        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, java.util.List.class)))
            |      );
            |    } else {
            |      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
            |    }
            |  }
            |  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
            |    switch (response.statusCode()) {
            |        case 200 -> {
            |          return new Response200(response.body().map(it -> serialization.<java.util.List<TodoDto>>deserializeBody(it, Wirespec.getType(TodoDto.class, java.util.List.class))).orElseThrow(() -> new IllegalStateException("body is null")));
            |        }
            |        default -> {
            |          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
            |        }
            |    }
            |  }
            |  public interface Handler extends Wirespec.Handler {
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
            |            return fromRawRequest(serialization, request);
            |        }
            |        @Override public Wirespec.RawResponse to(Response<?> response) {
            |            return toRawResponse(serialization, response);
            |        }
            |        };
            |      }
            |      @Override
            |      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
            |        return new Wirespec.ClientEdge<>() {
            |        @Override public Wirespec.RawRequest to(Request request) {
            |            return toRawRequest(serialization, request);
            |        }
            |        @Override public Response<?> from(Wirespec.RawResponse response) {
            |            return fromRawResponse(serialization, response);
            |        }
            |        };
            |      }
            |    };
            |  }
            |  public interface Call extends Wirespec.Call {
            |    public java.util.concurrent.CompletableFuture<Response<?>> getTodos();
            |  }
            |}
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TodoDto (
            |  String description
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.client;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TodoDto;
            |import community.flock.wirespec.generated.endpoint.GetTodos;
            |public record GetTodosClient (
            |  Wirespec.Serialization serialization,
            |  Wirespec.Transportation transportation
            |) implements GetTodos.Call {
            |  @Override
            |  public java.util.concurrent.CompletableFuture<GetTodos.Response<?>> getTodos() {
            |    final var request = new GetTodos.Request();
            |    final var rawRequest = GetTodos.toRawRequest(serialization(), request);
            |    return transportation().transport(rawRequest).thenApply(rawResponse -> GetTodos.fromRawResponse(serialization(), rawResponse));
            |  }
            |};
            |
            |package community.flock.wirespec.generated;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TodoDto;
            |import community.flock.wirespec.generated.endpoint.GetTodos;
            |import community.flock.wirespec.generated.client.GetTodosClient;
            |public record Client (
            |  Wirespec.Serialization serialization,
            |  Wirespec.Transportation transportation
            |) implements GetTodos.Call {
            |  @Override
            |  public java.util.concurrent.CompletableFuture<GetTodos.Response<?>> getTodos() {
            |    return new GetTodosClient(
            |      serialization(),
            |      transportation()
            |    ).getTodos();
            |  }
            |};
            |
        """.trimMargin()

        CompileMinimalEndpointTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileRefinedTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TodoId (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TodoNoRegex (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public String value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestInt (
            |  Long value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public Long value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestInt0 (
            |  Long value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public Long value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestInt1 (
            |  Long value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return 0 <= value;
            |  }
            |  @Override
            |  public Long value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestInt2 (
            |  Long value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return 1 <= value && value <= 3;
            |  }
            |  @Override
            |  public Long value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestNum (
            |  Double value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public Double value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestNum0 (
            |  Double value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public Double value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestNum1 (
            |  Double value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return value <= 0.5;
            |  }
            |  @Override
            |  public Double value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record TestNum2 (
            |  Double value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return -0.2 <= value && value <= 0.5;
            |  }
            |  @Override
            |  public Double value() {
            |    return value;
            |  }
            |};
            |
        """.trimMargin()

        CompileRefinedTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileUnionTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |public sealed interface UserAccount permits UserAccountPassword, UserAccountToken {}
            |
            |package community.flock.wirespec.generated.model;
            |public record UserAccountPassword (
            |  String username,
            |  String password
            |) implements Wirespec.Model, UserAccount {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |public record UserAccountToken (
            |  String token
            |) implements Wirespec.Model, UserAccount {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |public record User (
            |  String username,
            |  UserAccount account
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
        """.trimMargin()

        CompileUnionTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileTypeTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |public record Request (
            |  String type,
            |  String url,
            |  java.util.Optional<String> BODY_TYPE,
            |  java.util.List<String> params,
            |  java.util.Map<String, String> headers,
            |  java.util.Optional<java.util.Map<String, java.util.Optional<java.util.List<java.util.Optional<String>>>>> body
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.List.<String>of();
            |  }
            |};
            |
        """.trimMargin()

        CompileTypeTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileNestedTypeTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record DutchPostalCode (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^([0-9]{4}[A-Z]{2})${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Address (
            |  String street,
            |  Long houseNumber,
            |  DutchPostalCode postalCode
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return (!postalCode().validate() ? java.util.List.of("postalCode") : java.util.List.<String>of());
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Person (
            |  String name,
            |  Address address,
            |  java.util.List<String> tags
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return address().validate().stream().map(e -> "address." + e).toList();
            |  }
            |};
            |
        """.trimMargin()

        CompileNestedTypeTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun compileComplexModelTest() {
        val java = """
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Email (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record PhoneNumber (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^\\+[1-9]\\d{1,14}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Tag (
            |  String value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[a-z][a-z0-9-]{0,19}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record EmployeeAge (
            |  Long value
            |) implements Wirespec.Refined {
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Boolean validate() {
            |    return 18 <= value && value <= 65;
            |  }
            |  @Override
            |  public Long value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record ContactInfo (
            |  Email email,
            |  java.util.Optional<PhoneNumber> phone
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.stream.Stream.of((!email().validate() ? java.util.List.of("email") : java.util.List.<String>of()), phone().map(it -> (!it.validate() ? java.util.List.of("phone") : java.util.List.<String>of())).orElse(java.util.List.<String>of())).flatMap(java.util.Collection::stream).toList();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Employee (
            |  String name,
            |  EmployeeAge age,
            |  ContactInfo contactInfo,
            |  java.util.List<Tag> tags
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.stream.Stream.of((!age().validate() ? java.util.List.of("age") : java.util.List.<String>of()), contactInfo().validate().stream().map(e -> "contactInfo." + e).toList(), java.util.stream.IntStream.range(0, tags().size()).mapToObj(i -> (!tags().get(i).validate() ? java.util.List.of("tags[" + i + "]") : java.util.List.<String>of())).flatMap(java.util.Collection::stream).toList()).flatMap(java.util.Collection::stream).toList();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Department (
            |  String name,
            |  java.util.List<Employee> employees
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.stream.IntStream.range(0, employees().size()).mapToObj(i -> employees().get(i).validate().stream().map(e -> "employees[" + i + "]." + e).toList()).flatMap(java.util.Collection::stream).toList();
            |  }
            |};
            |
            |package community.flock.wirespec.generated.model;
            |import community.flock.wirespec.java.Wirespec;
            |public record Company (
            |  String name,
            |  java.util.List<Department> departments
            |) implements Wirespec.Model {
            |  @Override
            |  public java.util.List<String> validate() {
            |    return java.util.stream.IntStream.range(0, departments().size()).mapToObj(i -> departments().get(i).validate().stream().map(e -> "departments[" + i + "]." + e).toList()).flatMap(java.util.Collection::stream).toList();
            |  }
            |};
            |
        """.trimMargin()

        CompileComplexModelTest.compiler { JavaIrEmitter() } shouldBeRight java
    }

    @Test
    fun sharedOutputTest() {
        val expected = """
            |package community.flock.wirespec.java;
            |import java.lang.reflect.Type;
            |import java.lang.reflect.ParameterizedType;
            |import java.util.List;
            |import java.util.Map;
            |public interface Wirespec {
            |  public interface Model {
            |    public java.util.List<String> validate();
            |  }
            |  public interface Enum {
            |    String label();
            |  }
            |  public interface Endpoint {
            |  }
            |  public interface Channel {
            |  }
            |  public interface Refined<T> {
            |    T value();
            |    public Boolean validate();
            |  }
            |  public interface Path {
            |  }
            |  public interface Queries {
            |  }
            |  public interface Headers {
            |  }
            |  public interface Handler {
            |  }
            |  public interface Call {
            |  }
            |  public enum Method {
            |      GET,
            |      PUT,
            |      POST,
            |      DELETE,
            |      OPTIONS,
            |      HEAD,
            |      PATCH,
            |      TRACE
            |    }  public interface Request<T> {
            |    Path path();
            |    Method method();
            |    Queries queries();
            |    Headers headers();
            |    T body();
            |    public interface Headers {
            |    }
            |  }
            |  public interface Response<T> {
            |    Integer status();
            |    Headers headers();
            |    T body();
            |    public interface Headers {
            |    }
            |  }
            |  public interface BodySerializer {
            |    public <T> byte[] serializeBody(T t, Type type);
            |  }
            |  public interface BodyDeserializer {
            |    public <T> T deserializeBody(byte[] raw, Type type);
            |  }
            |  public interface BodySerialization extends BodySerializer, BodyDeserializer {
            |  }
            |  public interface PathSerializer {
            |    public <T> String serializePath(T t, Type type);
            |  }
            |  public interface PathDeserializer {
            |    public <T> T deserializePath(String raw, Type type);
            |  }
            |  public interface PathSerialization extends PathSerializer, PathDeserializer {
            |  }
            |  public interface ParamSerializer {
            |    public <T> java.util.List<String> serializeParam(T value, Type type);
            |  }
            |  public interface ParamDeserializer {
            |    public <T> T deserializeParam(java.util.List<String> values, Type type);
            |  }
            |  public interface ParamSerialization extends ParamSerializer, ParamDeserializer {
            |  }
            |  public interface Serializer extends BodySerializer, PathSerializer, ParamSerializer {
            |  }
            |  public interface Deserializer extends BodyDeserializer, PathDeserializer, ParamDeserializer {
            |  }
            |  public interface Serialization extends Serializer, Deserializer {
            |  }
            |  public static record RawRequest (
            |    String method,
            |    java.util.List<String> path,
            |    java.util.Map<String, java.util.List<String>> queries,
            |    java.util.Map<String, java.util.List<String>> headers,
            |    java.util.Optional<byte[]> body
            |  ) {
            |  };
            |  public static record RawResponse (
            |    Integer statusCode,
            |    java.util.Map<String, java.util.List<String>> headers,
            |    java.util.Optional<byte[]> body
            |  ) {
            |  };
            |  public interface Transportation {
            |    public java.util.concurrent.CompletableFuture<RawResponse> transport(RawRequest request);
            |  }
            |  public interface ServerEdge<Req extends Request<?>, Res extends Response<?>> {
            |    public Req from(RawRequest request);
            |    public RawResponse to(Res response);
            |  }
            |  public interface ClientEdge<Req extends Request<?>, Res extends Response<?>> {
            |    public RawRequest to(Req request);
            |    public Res from(RawResponse response);
            |  }
            |  public interface Client<Req extends Request<?>, Res extends Response<?>> {
            |    public String getPathTemplate();
            |    public String getMethod();
            |    public ClientEdge<Req, Res> getClient(Serialization serialization);
            |  }
            |  public interface Server<Req extends Request<?>, Res extends Response<?>> {
            |    public String getPathTemplate();
            |    public String getMethod();
            |    public ServerEdge<Req, Res> getServer(Serialization serialization);
            |  }
            |  public static Type getType(final Class<?> actualTypeArguments, final Class<?> rawType) {
            |    if(rawType != null) {
            |      return new ParameterizedType() {
            |        public Type getRawType() { return rawType; }
            |        public Type[] getActualTypeArguments() { return new Class<?>[]{actualTypeArguments}; }
            |        public Type getOwnerType() { return null; }
            |      };
            |    }
            |    else { return actualTypeArguments; }
            |  }}
            |
        """.trimMargin()

        val emitter = JavaIrEmitter()
        emitter.shared!!.source shouldBe expected
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
