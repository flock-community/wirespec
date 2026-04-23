package community.flock.wirespec.emitters.java

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.Definition
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Refined
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.core.parse.ast.Union
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
import community.flock.wirespec.compiler.utils.noLogger
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
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String toString() {
            |    return value;
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
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface PotentialTodoDtoGenerator {
            |  public static PotentialTodoDto generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new PotentialTodoDto(
            |      generator.generate((path + "name"), PotentialTodoDto.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "done"), PotentialTodoDto.class, new Wirespec.GeneratorFieldBoolean())
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TokenGenerator {
            |  public static Token generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Token(generator.generate((path + "iss"), Token.class, new Wirespec.GeneratorFieldString(null)));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TodoDtoGenerator {
            |  public static TodoDto generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TodoDto(
            |      generator.generate((path + "id"), TodoDto.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "name"), TodoDto.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "done"), TodoDto.class, new Wirespec.GeneratorFieldBoolean())
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface ErrorGenerator {
            |  public static Error generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Error(
            |      generator.generate((path + "code"), Error.class, new Wirespec.GeneratorFieldInteger(
            |        null,
            |        null
            |      )),
            |      generator.generate((path + "description"), Error.class, new Wirespec.GeneratorFieldString(null))
            |    );
            |  }
            |}
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
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface MyAwesomeEnumGenerator {
            |  public static MyAwesomeEnum generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new MyAwesomeEnum(generator.generate((path + "value"), MyAwesomeEnum.class, new Wirespec.GeneratorFieldEnum(java.util.List.of("ONE", "Two", "THREE_MORE", "UnitedKingdom", "-1", "0", "10", "-999", "88"))));
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
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TodoDtoGenerator {
            |  public static TodoDto generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TodoDto(generator.generate((path + "description"), TodoDto.class, new Wirespec.GeneratorFieldString(null)));
            |  }
            |}
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
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String toString() {
            |    return value;
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
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public String toString() {
            |    return value;
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
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |  public Boolean validate() {
            |    return 0 <= value;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |  public Boolean validate() {
            |    return 1 <= value && value <= 3;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |  public Boolean validate() {
            |    return true;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |  public Boolean validate() {
            |    return value <= 0.5;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |  public Boolean validate() {
            |    return -0.2 <= value && value <= 0.5;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
            |  }
            |  @Override
            |  public Double value() {
            |    return value;
            |  }
            |};
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TodoIdGenerator {
            |  public static TodoId generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TodoId(generator.generate((path + "value"), TodoId.class, new Wirespec.GeneratorFieldString("^[0-9a-fA-F]{8}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{4}\b-[0-9a-fA-F]{12}${'$'}")));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TodoNoRegexGenerator {
            |  public static TodoNoRegex generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TodoNoRegex(generator.generate((path + "value"), TodoNoRegex.class, new Wirespec.GeneratorFieldString(null)));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestIntGenerator {
            |  public static TestInt generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestInt(generator.generate((path + "value"), TestInt.class, new Wirespec.GeneratorFieldInteger(
            |      null,
            |      null
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestInt0Generator {
            |  public static TestInt0 generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestInt0(generator.generate((path + "value"), TestInt0.class, new Wirespec.GeneratorFieldInteger(
            |      null,
            |      null
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestInt1Generator {
            |  public static TestInt1 generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestInt1(generator.generate((path + "value"), TestInt1.class, new Wirespec.GeneratorFieldInteger(
            |      0L,
            |      null
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestInt2Generator {
            |  public static TestInt2 generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestInt2(generator.generate((path + "value"), TestInt2.class, new Wirespec.GeneratorFieldInteger(
            |      1L,
            |      3L
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestNumGenerator {
            |  public static TestNum generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestNum(generator.generate((path + "value"), TestNum.class, new Wirespec.GeneratorFieldNumber(
            |      null,
            |      null
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestNum0Generator {
            |  public static TestNum0 generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestNum0(generator.generate((path + "value"), TestNum0.class, new Wirespec.GeneratorFieldNumber(
            |      null,
            |      null
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestNum1Generator {
            |  public static TestNum1 generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestNum1(generator.generate((path + "value"), TestNum1.class, new Wirespec.GeneratorFieldNumber(
            |      null,
            |      0.5
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TestNum2Generator {
            |  public static TestNum2 generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new TestNum2(generator.generate((path + "value"), TestNum2.class, new Wirespec.GeneratorFieldNumber(
            |      -0.2,
            |      0.5
            |    )));
            |  }
            |}
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
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface UserAccountGenerator {
            |  public static UserAccount generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    final var variant = generator.generate((path + "variant"), UserAccount.class, new Wirespec.GeneratorFieldUnion(java.util.List.of("UserAccountPassword", "UserAccountToken")));
            |    switch (variant) {
            |        case "UserAccountPassword" -> {
            |          return UserAccountPasswordGenerator.generate((path + "UserAccountPassword"), generator);
            |        }
            |        case "UserAccountToken" -> {
            |          return UserAccountTokenGenerator.generate((path + "UserAccountToken"), generator);
            |        }
            |    }
            |    throw new IllegalStateException("Unknown variant");
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface UserAccountPasswordGenerator {
            |  public static UserAccountPassword generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new UserAccountPassword(
            |      generator.generate((path + "username"), UserAccountPassword.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "password"), UserAccountPassword.class, new Wirespec.GeneratorFieldString(null))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface UserAccountTokenGenerator {
            |  public static UserAccountToken generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new UserAccountToken(generator.generate((path + "token"), UserAccountToken.class, new Wirespec.GeneratorFieldString(null)));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface UserGenerator {
            |  public static User generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new User(
            |      generator.generate((path + "username"), User.class, new Wirespec.GeneratorFieldString(null)),
            |      UserAccountGenerator.generate((path + "account"), generator)
            |    );
            |  }
            |}
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
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface RequestGenerator {
            |  public static Request generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Request(
            |      generator.generate((path + "type"), Request.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "url"), Request.class, new Wirespec.GeneratorFieldString(null)),
            |      (generator.generate((path + "BODY_TYPE"), Request.class, new Wirespec.GeneratorFieldNullable(new Wirespec.GeneratorFieldString(null))) ? null : generator.generate((path + "BODY_TYPE"), Request.class, new Wirespec.GeneratorFieldString(null))),
            |      generator.generate((path + "params"), Request.class, new Wirespec.GeneratorFieldArray(new Wirespec.GeneratorFieldString(null))),
            |      generator.generate((path + "headers"), Request.class, new Wirespec.GeneratorFieldDict(
            |        null,
            |        new Wirespec.GeneratorFieldString(null)
            |      )),
            |      (generator.generate((path + "body"), Request.class, new Wirespec.GeneratorFieldNullable(new Wirespec.GeneratorFieldDict(
            |        null,
            |        null
            |      ))) ? null : generator.generate((path + "body"), Request.class, new Wirespec.GeneratorFieldDict(
            |        null,
            |        null
            |      )))
            |    );
            |  }
            |}
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
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^([0-9]{4}[A-Z]{2})${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String toString() {
            |    return value;
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
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface DutchPostalCodeGenerator {
            |  public static DutchPostalCode generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new DutchPostalCode(generator.generate((path + "value"), DutchPostalCode.class, new Wirespec.GeneratorFieldString("^([0-9]{4}[A-Z]{2})${'$'}")));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface AddressGenerator {
            |  public static Address generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Address(
            |      generator.generate((path + "street"), Address.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "houseNumber"), Address.class, new Wirespec.GeneratorFieldInteger(
            |        null,
            |        null
            |      )),
            |      DutchPostalCodeGenerator.generate((path + "postalCode"), generator)
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface PersonGenerator {
            |  public static Person generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Person(
            |      generator.generate((path + "name"), Person.class, new Wirespec.GeneratorFieldString(null)),
            |      AddressGenerator.generate((path + "address"), generator),
            |      generator.generate((path + "tags"), Person.class, new Wirespec.GeneratorFieldArray(new Wirespec.GeneratorFieldString(null)))
            |    );
            |  }
            |}
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
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String toString() {
            |    return value;
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
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^\\+[1-9]\\d{1,14}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String toString() {
            |    return value;
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
            |  public Boolean validate() {
            |    return java.util.regex.Pattern.compile("^[a-z][a-z0-9-]{0,19}${'$'}").matcher(value).find();
            |  }
            |  @Override
            |  public String toString() {
            |    return value;
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
            |  public Boolean validate() {
            |    return 18 <= value && value <= 65;
            |  }
            |  @Override
            |  public String toString() {
            |    return value.toString();
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
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface EmailGenerator {
            |  public static Email generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Email(generator.generate((path + "value"), Email.class, new Wirespec.GeneratorFieldString("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}${'$'}")));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface PhoneNumberGenerator {
            |  public static PhoneNumber generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new PhoneNumber(generator.generate((path + "value"), PhoneNumber.class, new Wirespec.GeneratorFieldString("^\+[1-9]\d{1,14}${'$'}")));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface TagGenerator {
            |  public static Tag generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Tag(generator.generate((path + "value"), Tag.class, new Wirespec.GeneratorFieldString("^[a-z][a-z0-9-]{0,19}${'$'}")));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface EmployeeAgeGenerator {
            |  public static EmployeeAge generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new EmployeeAge(generator.generate((path + "value"), EmployeeAge.class, new Wirespec.GeneratorFieldInteger(
            |      18L,
            |      65L
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface ContactInfoGenerator {
            |  public static ContactInfo generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new ContactInfo(
            |      EmailGenerator.generate((path + "email"), generator),
            |      (generator.generate((path + "phone"), ContactInfo.class, new Wirespec.GeneratorFieldNullable(null)) ? null : PhoneNumberGenerator.generate((path + "phone"), generator))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface EmployeeGenerator {
            |  public static Employee generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Employee(
            |      generator.generate((path + "name"), Employee.class, new Wirespec.GeneratorFieldString(null)),
            |      EmployeeAgeGenerator.generate((path + "age"), generator),
            |      ContactInfoGenerator.generate((path + "contactInfo"), generator),
            |      generator.generate((path + "tags"), Employee.class, new Wirespec.GeneratorFieldArray(null))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface DepartmentGenerator {
            |  public static Department generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Department(
            |      generator.generate((path + "name"), Department.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "employees"), Department.class, new Wirespec.GeneratorFieldArray(null))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface CompanyGenerator {
            |  public static Company generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Company(
            |      generator.generate((path + "name"), Company.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "departments"), Company.class, new Wirespec.GeneratorFieldArray(null))
            |    );
            |  }
            |}
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
            |  public sealed interface GeneratorField<T> {
            |  }
            |  public static record GeneratorFieldString (
            |    java.util.Optional<String> regex
            |  ) implements GeneratorField<String> {
            |  };
            |  public static record GeneratorFieldInteger (
            |    java.util.Optional<Long> min,
            |    java.util.Optional<Long> max
            |  ) implements GeneratorField<Long> {
            |  };
            |  public static record GeneratorFieldNumber (
            |    java.util.Optional<Double> min,
            |    java.util.Optional<Double> max
            |  ) implements GeneratorField<Double> {
            |  };
            |  public static record GeneratorFieldBoolean () implements GeneratorField<Boolean> {
            |  };
            |  public static record GeneratorFieldBytes () implements GeneratorField<byte[]> {
            |  };
            |  public static record GeneratorFieldEnum (
            |    java.util.List<String> values
            |  ) implements GeneratorField<String> {
            |  };
            |  public static record GeneratorFieldUnion (
            |    java.util.List<String> variants
            |  ) implements GeneratorField<String> {
            |  };
            |  public static record GeneratorFieldArray (
            |    java.util.Optional<GeneratorField<?>> inner
            |  ) implements GeneratorField<Integer> {
            |  };
            |  public static record GeneratorFieldNullable (
            |    java.util.Optional<GeneratorField<?>> inner
            |  ) implements GeneratorField<Boolean> {
            |  };
            |  public static record GeneratorFieldDict (
            |    java.util.Optional<GeneratorField<?>> key,
            |    java.util.Optional<GeneratorField<?>> value
            |  ) implements GeneratorField<Integer> {
            |  };
            |  public interface Generator {
            |    public <T> T generate(java.util.List<String> path, Type type, GeneratorField<T> field);
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
        emitter.shared.source shouldBe expected
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

    private fun emitGeneratorSource(node: Definition, fileNameSubstring: String): String {
        val emitter = JavaIrEmitter()
        val ast = AST(
            nonEmptyListOf(
                Module(
                    FileUri(""),
                    nonEmptyListOf(node),
                ),
            ),
        )
        val emitted = emitter.emit(ast, noLogger)
        val match = emitted.toList().first { it.file.contains(fileNameSubstring) }
        return match.result
    }

    @Test
    fun testEmitGeneratorForType() {
        val address = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Address"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("street"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                    Field(
                        identifier = FieldIdentifier("number"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.Integer(constraint = null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface AddressGenerator {
            |  public static Address generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Address(
            |      generator.generate((path + "street"), Address.class, new Wirespec.GeneratorFieldString(null)),
            |      generator.generate((path + "number"), Address.class, new Wirespec.GeneratorFieldInteger(
            |        null,
            |        null
            |      ))
            |    );
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(address, "AddressGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForEnum() {
        val color = Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Color"),
            entries = setOf("RED", "GREEN", "BLUE"),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface ColorGenerator {
            |  public static Color generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Color(generator.generate((path + "value"), Color.class, new Wirespec.GeneratorFieldEnum(java.util.List.of("RED", "GREEN", "BLUE"))));
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(color, "ColorGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForUnion() {
        val shape = Union(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Shape"),
            entries = setOf(
                Reference.Custom(value = "Circle", isNullable = false),
                Reference.Custom(value = "Square", isNullable = false),
            ),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface ShapeGenerator {
            |  public static Shape generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    final var variant = generator.generate((path + "variant"), Shape.class, new Wirespec.GeneratorFieldUnion(java.util.List.of("Circle", "Square")));
            |    switch (variant) {
            |        case "Circle" -> {
            |          return CircleGenerator.generate((path + "Circle"), generator);
            |        }
            |        case "Square" -> {
            |          return SquareGenerator.generate((path + "Square"), generator);
            |        }
            |    }
            |    throw new IllegalStateException("Unknown variant");
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(shape, "ShapeGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForRefined() {
        val uuid = Refined(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("UUID"),
            reference = Reference.Primitive(
                type = Reference.Primitive.Type.String(
                    Reference.Primitive.Type.Constraint.RegExp("/^[0-9a-f]{8}${'$'}/g"),
                ),
                isNullable = false,
            ),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface UUIDGenerator {
            |  public static UUID generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new UUID(generator.generate((path + "value"), UUID.class, new Wirespec.GeneratorFieldString("^[0-9a-f]{8}${'$'}")));
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(uuid, "UUIDGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForArrayField() {
        val inventory = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Inventory"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("items"),
                        annotations = emptyList(),
                        reference = Reference.Iterable(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface InventoryGenerator {
            |  public static Inventory generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Inventory(generator.generate((path + "items"), Inventory.class, new Wirespec.GeneratorFieldArray(new Wirespec.GeneratorFieldInteger(
            |      null,
            |      null
            |    ))));
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(inventory, "InventoryGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForDictField() {
        val lookup = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Lookup"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("entries"),
                        annotations = emptyList(),
                        reference = Reference.Dict(
                            reference = Reference.Primitive(
                                type = Reference.Primitive.Type.Integer(constraint = null),
                                isNullable = false,
                            ),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface LookupGenerator {
            |  public static Lookup generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Lookup(generator.generate((path + "entries"), Lookup.class, new Wirespec.GeneratorFieldDict(
            |      null,
            |      new Wirespec.GeneratorFieldInteger(
            |        null,
            |        null
            |      )
            |    )));
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(lookup, "LookupGenerator") shouldBe expected
    }

    @Test
    fun testEmitGeneratorForNullableField() {
        val person = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Person"),
            shape = Type.Shape(
                value = listOf(
                    Field(
                        identifier = FieldIdentifier("nickname"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = true,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val expected = """
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |public interface PersonGenerator {
            |  public static Person generate(java.util.List<String> path, Wirespec.Generator generator) {
            |    return new Person((generator.generate((path + "nickname"), Person.class, new Wirespec.GeneratorFieldNullable(new Wirespec.GeneratorFieldString(null))) ? null : generator.generate((path + "nickname"), Person.class, new Wirespec.GeneratorFieldString(null))));
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
