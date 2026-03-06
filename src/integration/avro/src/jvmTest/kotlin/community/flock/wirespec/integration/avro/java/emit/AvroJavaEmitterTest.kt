package community.flock.wirespec.integration.avro.java.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.FileUri
import community.flock.wirespec.compiler.core.emit.EmitShared
import community.flock.wirespec.compiler.core.emit.PackageName
import community.flock.wirespec.compiler.core.parse.ast.AST
import community.flock.wirespec.compiler.core.parse.ast.DefinitionIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Enum
import community.flock.wirespec.compiler.core.parse.ast.Field
import community.flock.wirespec.compiler.core.parse.ast.FieldIdentifier
import community.flock.wirespec.compiler.core.parse.ast.Module
import community.flock.wirespec.compiler.core.parse.ast.Reference
import community.flock.wirespec.compiler.core.parse.ast.Type
import community.flock.wirespec.compiler.test.CompileChannelTest
import community.flock.wirespec.compiler.test.CompileEnumTest
import community.flock.wirespec.compiler.test.CompileFullEndpointTest
import community.flock.wirespec.compiler.test.CompileMinimalEndpointTest
import community.flock.wirespec.compiler.test.CompileRefinedTest
import community.flock.wirespec.compiler.test.CompileTypeTest
import community.flock.wirespec.compiler.test.CompileUnionTest
import community.flock.wirespec.compiler.utils.noLogger
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrowWithMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvroJavaEmitterTest {

    private val emitter = AvroEmitter(PackageName("packageName"), EmitShared(true))

    @Test
    fun emitRootFunctionBodyTest() {
        val type = Type(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Identifier"),
            shape = Type.Shape(
                listOf(
                    Field(
                        identifier = FieldIdentifier(name = "name"),
                        annotations = emptyList(),
                        reference = Reference.Primitive(
                            type = Reference.Primitive.Type.String(null),
                            isNullable = false,
                        ),
                    ),
                ),
            ),
            extends = emptyList(),
        )

        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(type))))
        val expected = //language=Java
            """
            |package packageName.avro;
            |
            |import packageName.model.Identifier;
            |
            |public class IdentifierAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Identifier\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
            |
            |  public static Identifier from(org.apache.avro.generic.GenericData.Record record) {
            |    return new Identifier(
            |      (String) record.get(0).toString()
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(Identifier data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.name());
            |    return record;
            |  }
            |}
            """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        assertEquals(expected, actual.find { it.file == "packageName/avro/IdentifierAvro.java" }?.result)
    }

    @Test
    fun emitEnumFunctionBodyTest() {
        val enum = Enum(
            comment = null,
            annotations = emptyList(),
            identifier = DefinitionIdentifier("Identifier"),
            entries = setOf("ONE", "TWO", "THREE"),
        )
        val ast = AST(nonEmptyListOf(Module(FileUri(""), nonEmptyListOf(enum))))
        val expected =
            //language=Java
            """
            |package packageName.avro;
            |
            |import packageName.model.Identifier;
            |
            |public class IdentifierAvro {
            |
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Identifier\",\"symbols\":[\"ONE\",\"TWO\",\"THREE\"]}");
            |  
            |  public static Identifier from(org.apache.avro.generic.GenericData.EnumSymbol record) {
            |    return Identifier.valueOf(record.toString());
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.EnumSymbol to(Identifier data) {
            |    return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
            |  }
            |}
            """.trimMargin()
        val actual = emitter.emit(ast, noLogger)
        assertEquals(expected, actual.find { it.file == "packageName/avro/IdentifierAvro.java" }?.result)
    }

    @Test
    fun compileFullEndpointTest() {
        val result = CompileFullEndpointTest.compiler { emitter }
        val expect =
            """
            |package packageName.endpoint;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import packageName.model.Token;
            |import packageName.model.Token;
            |import packageName.model.PotentialTodoDto;
            |import packageName.model.TodoDto;
            |import packageName.model.Error;
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
            |    java.util.Optional<Token> RefreshToken
            |  ) implements Wirespec.Request.Headers {}
            |
            |  record Request (
            |    Path path,
            |    Wirespec.Method method,
            |    Queries queries,
            |    RequestHeaders headers,
            |    PotentialTodoDto body
            |  ) implements Wirespec.Request<PotentialTodoDto> {
            |    public Request(String id, Boolean done, java.util.Optional<String> name, Token token, java.util.Optional<Token> RefreshToken, PotentialTodoDto body) {
            |      this(new Path(id), Wirespec.Method.PUT, new Queries(done, name), new RequestHeaders(token, RefreshToken), body);
            |    }
            |  }
            |
            |  sealed interface Response<T> extends Wirespec.Response<T> {}
            |  sealed interface Response2XX<T> extends Response<T> {}
            |  sealed interface Response5XX<T> extends Response<T> {}
            |  sealed interface ResponseTodoDto extends Response<TodoDto> {}
            |  sealed interface ResponseError extends Response<Error> {}
            |
            |  record Response200(
            |    Integer status,
            |    Headers headers,
            |    TodoDto body
            |  ) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    public Response200(TodoDto body) {
            |      this(200, new Headers(), body);
            |    }
            |    static class Headers implements Wirespec.Response.Headers {}
            |  }
            |  record Response201(
            |    Integer status,
            |    Headers headers,
            |    TodoDto body
            |  ) implements Response2XX<TodoDto>, ResponseTodoDto {
            |    public Response201(Token token, java.util.Optional<Token> refreshToken, TodoDto body) {
            |      this(201, new Headers(token, refreshToken), body);
            |    }
            |    public record Headers(
            |    Token token,
            |    java.util.Optional<Token> refreshToken
            |  ) implements Wirespec.Response.Headers {}
            |  }
            |  record Response500(
            |    Integer status,
            |    Headers headers,
            |    Error body
            |  ) implements Response5XX<Error>, ResponseError {
            |    public Response500(Error body) {
            |      this(500, new Headers(), body);
            |    }
            |    static class Headers implements Wirespec.Response.Headers {}
            |  }
            |
            |  interface Handler extends Wirespec.Handler {
            |
            |    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method().name(),
            |        java.util.List.of("todos", serialization.serializePath(request.path().id(), Wirespec.getType(String.class, null))),
            |        java.util.Map.ofEntries(java.util.Map.entry("done", serialization.serializeParam(request.queries().done(), Wirespec.getType(Boolean.class, null))), java.util.Map.entry("name", serialization.serializeParam(request.queries().name(), Wirespec.getType(String.class, java.util.Optional.class)))),
            |        java.util.Map.ofEntries(java.util.Map.entry("token", serialization.serializeParam(request.headers().token(), Wirespec.getType(Token.class, null))), java.util.Map.entry("Refresh-Token", serialization.serializeParam(request.headers().RefreshToken(), Wirespec.getType(Token.class, java.util.Optional.class)))),
            |        java.util.Optional.ofNullable(serialization.serializeBody(request.body(), Wirespec.getType(PotentialTodoDto.class, null)))
            |      );
            |    }
            |
            |    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
            |      return new Request(
            |        serialization.deserializePath(request.path().get(1), Wirespec.getType(String.class, null)),
            |        serialization.<Boolean>deserializeParam(request.queries().getOrDefault("done", java.util.Collections.emptyList()), Wirespec.getType(Boolean.class, null)),
            |        serialization.<java.util.Optional<String>>deserializeParam(request.queries().getOrDefault("name", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
            |        serialization.<Token>deserializeParam(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("token")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(Token.class, null)),
            |        serialization.<java.util.Optional<Token>>deserializeParam(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Refresh-Token")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(Token.class, java.util.Optional.class)),
            |        request.body().<PotentialTodoDto>map(body -> serialization.deserializeBody(body, Wirespec.getType(PotentialTodoDto.class, null))).orElse(null)
            |      );
            |    }
            |
            |    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
            |      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, null)))); }
            |      if (response instanceof Response201 r) { return new Wirespec.RawResponse(r.status(), java.util.Map.ofEntries(java.util.Map.entry("token", serialization.<Token>serializeParam(r.headers().token(), Wirespec.getType(Token.class, null))), java.util.Map.entry("refreshToken", serialization.<java.util.Optional<Token>>serializeParam(r.headers().refreshToken(), Wirespec.getType(Token.class, java.util.Optional.class)))), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, null)))); }
            |      if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Error.class, null)))); }
            |      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
            |    }
            |
            |    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
            |      return switch (response.statusCode()) {
            |        case 200 -> new Response200(
            |          response.body().<TodoDto>map(body -> serialization.deserializeBody(body, Wirespec.getType(TodoDto.class, null))).orElse(null)
            |        );
            |        case 201 -> new Response201(
            |          serialization.<Token>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("token")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(Token.class, null)),
            |          serialization.<java.util.Optional<Token>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("refreshToken")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(Token.class, java.util.Optional.class)),
            |          response.body().<TodoDto>map(body -> serialization.deserializeBody(body, Wirespec.getType(TodoDto.class, null))).orElse(null)
            |        );
            |        case 500 -> new Response500(
            |          response.body().<Error>map(body -> serialization.deserializeBody(body, Wirespec.getType(Error.class, null))).orElse(null)
            |        );
            |        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
            |      };
            |    }
            |
            |    java.util.concurrent.CompletableFuture<Response<?>> putTodo(Request request);
            |    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override public String getPathTemplate() { return "/todos/{id}"; }
            |      @Override public String getMethod() { return "PUT"; }
            |      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
            |        return new Wirespec.ServerEdge<>() {
            |          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
            |          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
            |        };
            |      }
            |      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
            |        return new Wirespec.ClientEdge<>() {
            |          @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
            |          @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
            |        };
            |      }
            |    }
            |  }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record PotentialTodoDto (
            |  String name,
            |  Boolean done
            |) {
            |};
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record Token (
            |  String iss
            |) {
            |};
            |
            |package packageName.model;
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
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record Error (
            |  Long code,
            |  String description
            |) {
            |};
            |
            |package packageName.avro;
            |
            |import packageName.model.PotentialTodoDto;
            |
            |public class PotentialTodoDtoAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"PotentialTodoDto\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"done\",\"type\":\"boolean\"}]}");
            |
            |  public static PotentialTodoDto from(org.apache.avro.generic.GenericData.Record record) {
            |    return new PotentialTodoDto(
            |      (String) record.get(0).toString(),
            |      (Boolean) record.get(1)
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(PotentialTodoDto data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.name());
            |      record.put(1, data.done());
            |    return record;
            |  }
            |}
            |package packageName.avro;
            |
            |import packageName.model.Token;
            |
            |public class TokenAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Token\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"iss\",\"type\":\"string\"}]}");
            |
            |  public static Token from(org.apache.avro.generic.GenericData.Record record) {
            |    return new Token(
            |      (String) record.get(0).toString()
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(Token data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.iss());
            |    return record;
            |  }
            |}
            |package packageName.avro;
            |
            |import packageName.model.TodoDto;
            |
            |public class TodoDtoAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TodoDto\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"done\",\"type\":\"boolean\"}]}");
            |
            |  public static TodoDto from(org.apache.avro.generic.GenericData.Record record) {
            |    return new TodoDto(
            |      (String) record.get(0).toString(),
            |      (String) record.get(1).toString(),
            |      (Boolean) record.get(2)
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(TodoDto data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.id());
            |      record.put(1, data.name());
            |      record.put(2, data.done());
            |    return record;
            |  }
            |}
            |package packageName.avro;
            |
            |import packageName.model.Error;
            |
            |public class ErrorAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Error\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"code\",\"type\":\"long\"},{\"name\":\"description\",\"type\":\"string\"}]}");
            |
            |  public static Error from(org.apache.avro.generic.GenericData.Record record) {
            |    return new Error(
            |      (Long) record.get(0),
            |      (String) record.get(1).toString()
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(Error data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.code());
            |      record.put(1, data.description());
            |    return record;
            |  }
            |}
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileMinimalEndpointTest() {
        val result = CompileMinimalEndpointTest.compiler { emitter }
        val expect =
            """
            |package packageName.endpoint;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import packageName.model.TodoDto;
            |
            |public interface GetTodos extends Wirespec.Endpoint {
            |  static class Path implements Wirespec.Path {}
            |
            |  static class Queries implements Wirespec.Queries {}
            |
            |  static class RequestHeaders implements Wirespec.Request.Headers {}
            |
            |  record Request (
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
            |
            |  sealed interface Response<T> extends Wirespec.Response<T> {}
            |  sealed interface Response2XX<T> extends Response<T> {}
            |  sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> {}
            |
            |  record Response200(
            |    Integer status,
            |    Headers headers,
            |    java.util.List<TodoDto> body
            |  ) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
            |    public Response200(java.util.List<TodoDto> body) {
            |      this(200, new Headers(), body);
            |    }
            |    static class Headers implements Wirespec.Response.Headers {}
            |  }
            |
            |  interface Handler extends Wirespec.Handler {
            |
            |    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
            |      return new Wirespec.RawRequest(
            |        request.method().name(),
            |        java.util.List.of("todos"),
            |        java.util.Collections.emptyMap(),
            |        java.util.Collections.emptyMap(),
            |        java.util.Optional.empty()
            |      );
            |    }
            |
            |    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
            |      return new Request();
            |    }
            |
            |    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
            |      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, java.util.List.class)))); }
            |      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
            |    }
            |
            |    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
            |      if (response.statusCode() == 200) {
            |        return new Response200(
            |          response.body().<java.util.List<TodoDto>>map(body -> serialization.deserializeBody(body, Wirespec.getType(TodoDto.class, java.util.List.class))).orElse(null)
            |        );
            |      } else {
            |        throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
            |      }
            |    }
            |
            |    java.util.concurrent.CompletableFuture<Response<?>> getTodos(Request request);
            |    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override public String getPathTemplate() { return "/todos"; }
            |      @Override public String getMethod() { return "GET"; }
            |      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
            |        return new Wirespec.ServerEdge<>() {
            |          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
            |          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
            |        };
            |      }
            |      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
            |        return new Wirespec.ClientEdge<>() {
            |          @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
            |          @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
            |        };
            |      }
            |    }
            |  }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoDto (
            |  String description
            |) {
            |};
            |
            |package packageName.avro;
            |
            |import packageName.model.TodoDto;
            |
            |public class TodoDtoAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TodoDto\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"description\",\"type\":\"string\"}]}");
            |
            |  public static TodoDto from(org.apache.avro.generic.GenericData.Record record) {
            |    return new TodoDto(
            |      (String) record.get(0).toString()
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(TodoDto data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.description());
            |    return record;
            |  }
            |}
            """.trimMargin()

        result.shouldBeRight(expect)
    }

    @Test
    fun compileChannelTest() {
        val result = CompileChannelTest.compiler { emitter }
        val expect =
            //language=Java
            """
            |package packageName.channel;
            |
            |
            |
            |@FunctionalInterface
            |public interface Queue {
            |   void invoke(String message);
            |}
            |
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileEnumTest() {
        val result = CompileEnumTest.compiler { emitter }
        val expect =
            //language=Java
            """
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public enum MyAwesomeEnum implements Wirespec.Enum {
            |  ONE("ONE"),
            |  Two("Two"),
            |  THREE_MORE("THREE_MORE"),
            |  UnitedKingdom("UnitedKingdom"),
            |  __1("-1"),
            |  _0("0"),
            |  _10("10"),
            |  __999("-999"),
            |  _88("88");
            |  public final String label;
            |  MyAwesomeEnum(String label) {
            |    this.label = label;
            |  }
            |  @Override
            |  public String toString() {
            |    return label;
            |  }
            |  @Override
            |  public String label() {
            |    return label;
            |  }
            |}
            |
            |package packageName.avro;
            |
            |import packageName.model.MyAwesomeEnum;
            |
            |public class MyAwesomeEnumAvro {
            |
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"MyAwesomeEnum\",\"symbols\":[\"ONE\",\"Two\",\"THREE_MORE\",\"UnitedKingdom\",\"-1\",\"0\",\"10\",\"-999\",\"88\"]}");
            |  
            |  public static MyAwesomeEnum from(org.apache.avro.generic.GenericData.EnumSymbol record) {
            |    return MyAwesomeEnum.valueOf(record.toString());
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.EnumSymbol to(MyAwesomeEnum data) {
            |    return new org.apache.avro.generic.GenericData.EnumSymbol(SCHEMA, data.name());
            |  }
            |}
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileRefinedTest() {
        val result = CompileRefinedTest.compiler { emitter }
        val expect =
            //language=Java
            """
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoId (String value) implements Wirespec.Refined<String> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TodoId record) {
            |    return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$").matcher(record.value).find();
            |  }
            |  @Override
            |  public String value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoNoRegex (String value) implements Wirespec.Refined<String> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TodoNoRegex record) {
            |    return true;
            |  }
            |  @Override
            |  public String value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestInt (Long value) implements Wirespec.Refined<Long> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestInt record) {
            |    return true;
            |  }
            |  @Override
            |  public Long value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestInt0 (Long value) implements Wirespec.Refined<Long> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestInt0 record) {
            |    return true;
            |  }
            |  @Override
            |  public Long value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestInt1 (Long value) implements Wirespec.Refined<Long> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestInt1 record) {
            |    return 0 < record.value;
            |  }
            |  @Override
            |  public Long value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestInt2 (Long value) implements Wirespec.Refined<Long> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestInt2 record) {
            |    return 3 < record.value && record.value < 1;
            |  }
            |  @Override
            |  public Long value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestNum (Double value) implements Wirespec.Refined<Double> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestNum record) {
            |    return true;
            |  }
            |  @Override
            |  public Double value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestNum0 (Double value) implements Wirespec.Refined<Double> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestNum0 record) {
            |    return true;
            |  }
            |  @Override
            |  public Double value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestNum1 (Double value) implements Wirespec.Refined<Double> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestNum1 record) {
            |    return record.value < 0.5;
            |  }
            |  @Override
            |  public Double value() { return value; }
            |}
            |
            |package packageName.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TestNum2 (Double value) implements Wirespec.Refined<Double> {
            |  @Override
            |  public String toString() { return value.toString(); }
            |  public static boolean validate(TestNum2 record) {
            |    return -0.2 < record.value && record.value < 0.5;
            |  }
            |  @Override
            |  public Double value() { return value; }
            |}
            |
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileUnionTest() {
        val result = CompileUnionTest.compiler { emitter }
        val expect =
            //language=Java
            """
            |package packageName.model;
            |
            |public sealed interface UserAccount permits UserAccountPassword, UserAccountToken {}
            |
            |package packageName.model;
            |
            |public record UserAccountPassword (
            |  String username,
            |  String password
            |) implements UserAccount {
            |};
            |
            |package packageName.model;
            |
            |public record UserAccountToken (
            |  String token
            |) implements UserAccount {
            |};
            |
            |package packageName.model;
            |
            |public record User (
            |  String username,
            |  UserAccount account
            |) {
            |};
            |
            |package packageName.avro;
            |
            |import packageName.model.UserAccountPassword;
            |
            |public class UserAccountPasswordAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UserAccountPassword\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"username\",\"type\":\"string\"},{\"name\":\"password\",\"type\":\"string\"}]}");
            |
            |  public static UserAccountPassword from(org.apache.avro.generic.GenericData.Record record) {
            |    return new UserAccountPassword(
            |      (String) record.get(0).toString(),
            |      (String) record.get(1).toString()
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(UserAccountPassword data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.username());
            |      record.put(1, data.password());
            |    return record;
            |  }
            |}
            |package packageName.avro;
            |
            |import packageName.model.UserAccountToken;
            |
            |public class UserAccountTokenAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"UserAccountToken\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"token\",\"type\":\"string\"}]}");
            |
            |  public static UserAccountToken from(org.apache.avro.generic.GenericData.Record record) {
            |    return new UserAccountToken(
            |      (String) record.get(0).toString()
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(UserAccountToken data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.token());
            |    return record;
            |  }
            |}
            |package packageName.avro;
            |
            |import packageName.model.User;
            |
            |public class UserAvro {
            |  
            |  public static final org.apache.avro.Schema SCHEMA = 
            |    new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"packageName\",\"fields\":[{\"name\":\"username\",\"type\":\"string\"},{\"name\":\"account\",\"type\":" + UserAccountAvro.SCHEMA + "}]}");
            |
            |  public static User from(org.apache.avro.generic.GenericData.Record record) {
            |    return new User(
            |      (String) record.get(0).toString(),
            |      UserAccountAvro.from((org.apache.avro.generic.GenericData.Record) record.get(1))
            |    );
            |  }
            |  
            |  public static org.apache.avro.generic.GenericData.Record to(User data) {
            |    var record = new org.apache.avro.generic.GenericData.Record(SCHEMA);
            |      record.put(0, data.username());
            |      record.put(1, UserAccountAvro.to(data.account()));
            |    return record;
            |  }
            |}
            """.trimMargin()
        result.shouldBeRight(expect)
    }

    @Test
    fun compileTypeTest() {
        shouldThrowWithMessage<NotImplementedError>("An operation is not implemented.") { CompileTypeTest.compiler { emitter } }
    }
}
