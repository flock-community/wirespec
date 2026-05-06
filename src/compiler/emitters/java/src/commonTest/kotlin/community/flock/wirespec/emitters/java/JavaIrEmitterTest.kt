package community.flock.wirespec.emitters.java

import arrow.core.nonEmptyListOf
import arrow.core.nonEmptySetOf
import community.flock.wirespec.compiler.core.EmitContext
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
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
            |import community.flock.wirespec.generated.model.PotentialTodoDto;
            |public interface PotentialTodoDtoGenerator {
            |  public static PotentialTodoDto generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new PotentialTodoDto(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), PotentialTodoDto.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("done")).flatMap(java.util.Collection::stream).toList(), PotentialTodoDto.class, new Wirespec.GeneratorFieldBoolean(java.util.List.<java.util.Map<String, Object>>of()))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Token;
            |public interface TokenGenerator {
            |  public static Token generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Token(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("iss")).flatMap(java.util.Collection::stream).toList(), Token.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TodoDto;
            |public interface TodoDtoGenerator {
            |  public static TodoDto generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TodoDto(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("id")).flatMap(java.util.Collection::stream).toList(), TodoDto.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), TodoDto.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("done")).flatMap(java.util.Collection::stream).toList(), TodoDto.class, new Wirespec.GeneratorFieldBoolean(java.util.List.<java.util.Map<String, Object>>of()))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Error;
            |public interface ErrorGenerator {
            |  public static Error generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Error(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("code")).flatMap(java.util.Collection::stream).toList(), Error.class, new Wirespec.GeneratorFieldInteger(
            |        java.util.Optional.empty(),
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("description")).flatMap(java.util.Collection::stream).toList(), Error.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      ))
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
            |import community.flock.wirespec.generated.model.MyAwesomeEnum;
            |public interface MyAwesomeEnumGenerator {
            |  public static MyAwesomeEnum generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return MyAwesomeEnum.valueOf(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), MyAwesomeEnum.class, new Wirespec.GeneratorFieldEnum(
            |      java.util.List.of("ONE", "Two", "THREE_MORE", "UnitedKingdom", "-1", "0", "10", "-999", "88"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
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
            |import community.flock.wirespec.generated.model.TodoDto;
            |public interface TodoDtoGenerator {
            |  public static TodoDto generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TodoDto(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("description")).flatMap(java.util.Collection::stream).toList(), TodoDto.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
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
            |import community.flock.wirespec.generated.model.TodoId;
            |public interface TodoIdGenerator {
            |  public static TodoId generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TodoId(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TodoId.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.of("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}${'$'}"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TodoNoRegex;
            |public interface TodoNoRegexGenerator {
            |  public static TodoNoRegex generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TodoNoRegex(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TodoNoRegex.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestInt;
            |public interface TestIntGenerator {
            |  public static TestInt generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestInt(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestInt.class, new Wirespec.GeneratorFieldInteger(
            |      java.util.Optional.empty(),
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestInt0;
            |public interface TestInt0Generator {
            |  public static TestInt0 generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestInt0(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestInt0.class, new Wirespec.GeneratorFieldInteger(
            |      java.util.Optional.empty(),
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestInt1;
            |public interface TestInt1Generator {
            |  public static TestInt1 generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestInt1(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestInt1.class, new Wirespec.GeneratorFieldInteger(
            |      java.util.Optional.of(0L),
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestInt2;
            |public interface TestInt2Generator {
            |  public static TestInt2 generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestInt2(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestInt2.class, new Wirespec.GeneratorFieldInteger(
            |      java.util.Optional.of(1L),
            |      java.util.Optional.of(3L),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestNum;
            |public interface TestNumGenerator {
            |  public static TestNum generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestNum(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestNum.class, new Wirespec.GeneratorFieldNumber(
            |      java.util.Optional.empty(),
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestNum0;
            |public interface TestNum0Generator {
            |  public static TestNum0 generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestNum0(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestNum0.class, new Wirespec.GeneratorFieldNumber(
            |      java.util.Optional.empty(),
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestNum1;
            |public interface TestNum1Generator {
            |  public static TestNum1 generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestNum1(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestNum1.class, new Wirespec.GeneratorFieldNumber(
            |      java.util.Optional.empty(),
            |      java.util.Optional.of(0.5),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.TestNum2;
            |public interface TestNum2Generator {
            |  public static TestNum2 generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new TestNum2(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), TestNum2.class, new Wirespec.GeneratorFieldNumber(
            |      java.util.Optional.of(-0.2),
            |      java.util.Optional.of(0.5),
            |      java.util.List.<java.util.Map<String, Object>>of()
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
            |import community.flock.wirespec.generated.model.UserAccount;
            |public interface UserAccountGenerator {
            |  public static UserAccount generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    final var variant = generator.generate(java.util.stream.Stream.of(path, java.util.List.of("variant")).flatMap(java.util.Collection::stream).toList(), UserAccount.class, new Wirespec.GeneratorFieldUnion(
            |      java.util.List.of("UserAccountPassword", "UserAccountToken"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    ));
            |    switch (variant) {
            |        case "UserAccountPassword" -> {
            |          return UserAccountPasswordGenerator.generate(generator, java.util.stream.Stream.of(path, java.util.List.of("UserAccountPassword")).flatMap(java.util.Collection::stream).toList());
            |        }
            |        case "UserAccountToken" -> {
            |          return UserAccountTokenGenerator.generate(generator, java.util.stream.Stream.of(path, java.util.List.of("UserAccountToken")).flatMap(java.util.Collection::stream).toList());
            |        }
            |    }
            |    throw new IllegalStateException("Unknown variant");
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.UserAccountPassword;
            |public interface UserAccountPasswordGenerator {
            |  public static UserAccountPassword generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new UserAccountPassword(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("username")).flatMap(java.util.Collection::stream).toList(), UserAccountPassword.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("password")).flatMap(java.util.Collection::stream).toList(), UserAccountPassword.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      ))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.UserAccountToken;
            |public interface UserAccountTokenGenerator {
            |  public static UserAccountToken generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new UserAccountToken(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("token")).flatMap(java.util.Collection::stream).toList(), UserAccountToken.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.User;
            |public interface UserGenerator {
            |  public static User generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new User(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("username")).flatMap(java.util.Collection::stream).toList(), User.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("account")).flatMap(java.util.Collection::stream).toList(), User.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Collections.emptyMap(),
            |        (p0) -> UserAccountGenerator.generate(generator, p0)
            |      ))
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
            |import community.flock.wirespec.generated.model.Request;
            |public interface RequestGenerator {
            |  public static Request generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Request(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("type")).flatMap(java.util.Collection::stream).toList(), Request.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("url")).flatMap(java.util.Collection::stream).toList(), Request.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("BODY_TYPE")).flatMap(java.util.Collection::stream).toList(), Request.class, new Wirespec.GeneratorFieldNullable((p0) -> generator.generate(p0, Request.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )))),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("params")).flatMap(java.util.Collection::stream).toList(), Request.class, new Wirespec.GeneratorFieldArray((p0) -> generator.generate(p0, Request.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )))),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("headers")).flatMap(java.util.Collection::stream).toList(), Request.class, new Wirespec.GeneratorFieldDict((p0) -> generator.generate(p0, Request.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )))),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("body")).flatMap(java.util.Collection::stream).toList(), Request.class, new Wirespec.GeneratorFieldNullable((p0) -> generator.generate(p0, Request.class, new Wirespec.GeneratorFieldDict((p1) -> generator.generate(p1, Request.class, new Wirespec.GeneratorFieldArray((p2) -> generator.generate(p2, Request.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      ))))))))
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
            |import community.flock.wirespec.generated.model.DutchPostalCode;
            |public interface DutchPostalCodeGenerator {
            |  public static DutchPostalCode generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new DutchPostalCode(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), DutchPostalCode.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.of("^([0-9]{4}[A-Z]{2})${'$'}"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Address;
            |public interface AddressGenerator {
            |  public static Address generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Address(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("street")).flatMap(java.util.Collection::stream).toList(), Address.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("houseNumber")).flatMap(java.util.Collection::stream).toList(), Address.class, new Wirespec.GeneratorFieldInteger(
            |        java.util.Optional.empty(),
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("postalCode")).flatMap(java.util.Collection::stream).toList(), Address.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Collections.emptyMap(),
            |        (p0) -> DutchPostalCodeGenerator.generate(generator, p0)
            |      ))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Person;
            |public interface PersonGenerator {
            |  public static Person generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Person(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), Person.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("address")).flatMap(java.util.Collection::stream).toList(), Person.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Map.ofEntries(java.util.Map.entry("street", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("houseNumber", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("postalCode", java.util.List.<java.util.Map<String, Object>>of())),
            |        (p0) -> AddressGenerator.generate(generator, p0)
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("tags")).flatMap(java.util.Collection::stream).toList(), Person.class, new Wirespec.GeneratorFieldArray((p0) -> generator.generate(p0, Person.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      ))))
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
            |import community.flock.wirespec.generated.model.Email;
            |public interface EmailGenerator {
            |  public static Email generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Email(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), Email.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.of("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}${'$'}"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.PhoneNumber;
            |public interface PhoneNumberGenerator {
            |  public static PhoneNumber generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new PhoneNumber(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), PhoneNumber.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.of("^\\+[1-9]\\d{1,14}${'$'}"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Tag;
            |public interface TagGenerator {
            |  public static Tag generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Tag(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), Tag.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.of("^[a-z][a-z0-9-]{0,19}${'$'}"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.EmployeeAge;
            |public interface EmployeeAgeGenerator {
            |  public static EmployeeAge generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new EmployeeAge(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), EmployeeAge.class, new Wirespec.GeneratorFieldInteger(
            |      java.util.Optional.of(18L),
            |      java.util.Optional.of(65L),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.ContactInfo;
            |public interface ContactInfoGenerator {
            |  public static ContactInfo generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new ContactInfo(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("email")).flatMap(java.util.Collection::stream).toList(), ContactInfo.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Collections.emptyMap(),
            |        (p0) -> EmailGenerator.generate(generator, p0)
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("phone")).flatMap(java.util.Collection::stream).toList(), ContactInfo.class, new Wirespec.GeneratorFieldNullable((p0) -> generator.generate(p0, ContactInfo.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Collections.emptyMap(),
            |        (p1) -> PhoneNumberGenerator.generate(generator, p1)
            |      ))))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Employee;
            |public interface EmployeeGenerator {
            |  public static Employee generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Employee(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), Employee.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("age")).flatMap(java.util.Collection::stream).toList(), Employee.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Collections.emptyMap(),
            |        (p0) -> EmployeeAgeGenerator.generate(generator, p0)
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("contactInfo")).flatMap(java.util.Collection::stream).toList(), Employee.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Map.ofEntries(java.util.Map.entry("email", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("phone", java.util.List.<java.util.Map<String, Object>>of())),
            |        (p0) -> ContactInfoGenerator.generate(generator, p0)
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("tags")).flatMap(java.util.Collection::stream).toList(), Employee.class, new Wirespec.GeneratorFieldArray((p0) -> generator.generate(p0, Employee.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Collections.emptyMap(),
            |        (p1) -> TagGenerator.generate(generator, p1)
            |      ))))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Department;
            |public interface DepartmentGenerator {
            |  public static Department generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Department(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), Department.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("employees")).flatMap(java.util.Collection::stream).toList(), Department.class, new Wirespec.GeneratorFieldArray((p0) -> generator.generate(p0, Department.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Map.ofEntries(java.util.Map.entry("name", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("age", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("contactInfo", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("tags", java.util.List.<java.util.Map<String, Object>>of())),
            |        (p1) -> EmployeeGenerator.generate(generator, p1)
            |      ))))
            |    );
            |  }
            |}
            |
            |package community.flock.wirespec.generated.generator;
            |import community.flock.wirespec.java.Wirespec;
            |import community.flock.wirespec.generated.model.Company;
            |public interface CompanyGenerator {
            |  public static Company generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Company(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("name")).flatMap(java.util.Collection::stream).toList(), Company.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("departments")).flatMap(java.util.Collection::stream).toList(), Company.class, new Wirespec.GeneratorFieldArray((p0) -> generator.generate(p0, Company.class, new Wirespec.GeneratorFieldShape(
            |        java.util.Map.ofEntries(java.util.Map.entry("name", java.util.List.<java.util.Map<String, Object>>of()), java.util.Map.entry("employees", java.util.List.<java.util.Map<String, Object>>of())),
            |        (p1) -> DepartmentGenerator.generate(generator, p1)
            |      ))))
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
            |    java.util.Optional<String> regex,
            |    java.util.List<java.util.Map<String, Object>> annotations
            |  ) implements GeneratorField<String> {
            |  };
            |  public static record GeneratorFieldInteger (
            |    java.util.Optional<Long> min,
            |    java.util.Optional<Long> max,
            |    java.util.List<java.util.Map<String, Object>> annotations
            |  ) implements GeneratorField<Long> {
            |  };
            |  public static record GeneratorFieldNumber (
            |    java.util.Optional<Double> min,
            |    java.util.Optional<Double> max,
            |    java.util.List<java.util.Map<String, Object>> annotations
            |  ) implements GeneratorField<Double> {
            |  };
            |  public static record GeneratorFieldBoolean (
            |    java.util.List<java.util.Map<String, Object>> annotations
            |  ) implements GeneratorField<Boolean> {
            |  };
            |  public static record GeneratorFieldBytes (
            |    java.util.List<java.util.Map<String, Object>> annotations
            |  ) implements GeneratorField<byte[]> {
            |  };
            |  public static record GeneratorFieldEnum (
            |    java.util.List<String> values,
            |    java.util.List<java.util.Map<String, Object>> annotations
            |  ) implements GeneratorField<String> {
            |  };
            |  public static record GeneratorFieldUnion (
            |    java.util.List<String> variants,
            |    java.util.List<java.util.Map<String, Object>> annotations
            |  ) implements GeneratorField<String> {
            |  };
            |  public static record GeneratorFieldArray<T> (
            |    java.util.function.Function<java.util.List<String>, T> generate
            |  ) implements GeneratorField<java.util.List<T>> {
            |  };
            |  public static record GeneratorFieldNullable<T> (
            |    java.util.function.Function<java.util.List<String>, T> generate
            |  ) implements GeneratorField<java.util.Optional<T>> {
            |  };
            |  public static record GeneratorFieldShape<T> (
            |    java.util.Map<String, java.util.List<java.util.Map<String, Object>>> annotations,
            |    java.util.function.Function<java.util.List<String>, T> generate
            |  ) implements GeneratorField<T> {
            |  };
            |  public static record GeneratorFieldDict<V> (
            |    java.util.function.Function<java.util.List<String>, V> generate
            |  ) implements GeneratorField<java.util.Map<String, V>> {
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
            |import community.flock.wirespec.generated.model.Address;
            |public interface AddressGenerator {
            |  public static Address generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Address(
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("street")).flatMap(java.util.Collection::stream).toList(), Address.class, new Wirespec.GeneratorFieldString(
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
            |      )),
            |      generator.generate(java.util.stream.Stream.of(path, java.util.List.of("number")).flatMap(java.util.Collection::stream).toList(), Address.class, new Wirespec.GeneratorFieldInteger(
            |        java.util.Optional.empty(),
            |        java.util.Optional.empty(),
            |        java.util.List.<java.util.Map<String, Object>>of()
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
            |import community.flock.wirespec.generated.model.Color;
            |public interface ColorGenerator {
            |  public static Color generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return Color.valueOf(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), Color.class, new Wirespec.GeneratorFieldEnum(
            |      java.util.List.of("RED", "GREEN", "BLUE"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
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
            |import community.flock.wirespec.generated.model.Shape;
            |public interface ShapeGenerator {
            |  public static Shape generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    final var variant = generator.generate(java.util.stream.Stream.of(path, java.util.List.of("variant")).flatMap(java.util.Collection::stream).toList(), Shape.class, new Wirespec.GeneratorFieldUnion(
            |      java.util.List.of("Circle", "Square"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    ));
            |    switch (variant) {
            |        case "Circle" -> {
            |          return CircleGenerator.generate(generator, java.util.stream.Stream.of(path, java.util.List.of("Circle")).flatMap(java.util.Collection::stream).toList());
            |        }
            |        case "Square" -> {
            |          return SquareGenerator.generate(generator, java.util.stream.Stream.of(path, java.util.List.of("Square")).flatMap(java.util.Collection::stream).toList());
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
            |import community.flock.wirespec.generated.model.UUID;
            |public interface UUIDGenerator {
            |  public static UUID generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new UUID(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("value")).flatMap(java.util.Collection::stream).toList(), UUID.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.of("^[0-9a-f]{8}${'$'}"),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )));
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
            |import community.flock.wirespec.generated.model.Inventory;
            |public interface InventoryGenerator {
            |  public static Inventory generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Inventory(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("items")).flatMap(java.util.Collection::stream).toList(), Inventory.class, new Wirespec.GeneratorFieldArray((p0) -> generator.generate(p0, Inventory.class, new Wirespec.GeneratorFieldInteger(
            |      java.util.Optional.empty(),
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )))));
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
            |import community.flock.wirespec.generated.model.Lookup;
            |public interface LookupGenerator {
            |  public static Lookup generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Lookup(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("entries")).flatMap(java.util.Collection::stream).toList(), Lookup.class, new Wirespec.GeneratorFieldDict((p0) -> generator.generate(p0, Lookup.class, new Wirespec.GeneratorFieldInteger(
            |      java.util.Optional.empty(),
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )))));
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
            |import community.flock.wirespec.generated.model.Person;
            |public interface PersonGenerator {
            |  public static Person generate(Wirespec.Generator generator, java.util.List<String> path) {
            |    return new Person(generator.generate(java.util.stream.Stream.of(path, java.util.List.of("nickname")).flatMap(java.util.Collection::stream).toList(), Person.class, new Wirespec.GeneratorFieldNullable((p0) -> generator.generate(p0, Person.class, new Wirespec.GeneratorFieldString(
            |      java.util.Optional.empty(),
            |      java.util.List.<java.util.Map<String, Object>>of()
            |    )))));
            |  }
            |}
            |
        """.trimMargin()

        emitGeneratorSource(person, "PersonGenerator") shouldBe expected
    }
}
