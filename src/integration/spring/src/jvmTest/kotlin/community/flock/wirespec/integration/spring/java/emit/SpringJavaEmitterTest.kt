package community.flock.wirespec.integration.spring.java.emit

import arrow.core.nonEmptyListOf
import community.flock.wirespec.compiler.core.ModuleContent
import community.flock.wirespec.compiler.core.ParseContext
import community.flock.wirespec.compiler.core.WirespecSpec
import community.flock.wirespec.compiler.core.emit.common.PackageName
import community.flock.wirespec.compiler.core.parse
import community.flock.wirespec.compiler.core.parse.AST
import community.flock.wirespec.compiler.utils.NoLogger
import community.flock.wirespec.compiler.utils.noLogger
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals

class SpringJavaEmitterTest {

    private fun parse(source: String): AST = object : ParseContext, NoLogger {
        override val spec = WirespecSpec
    }.parse(nonEmptyListOf(ModuleContent("", source))).getOrNull() ?: error("Parsing failed.")

    @Test
    fun `Should emit the full wirespec, and add annotation to the handler method`() {
        val path = Path("src/jvmTest/resources/todo.ws")
        val text = SystemFileSystem.source(path).buffered().readString()

        val ast = parse(text)
        val actual = SpringJavaEmitter(PackageName("community.flock.wirespec.spring.test"))
            .emit(ast, noLogger)
            .map { it.result }
        val expected = listOf(
            """
            |package community.flock.wirespec.spring.test.model;
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
            """.trimMargin(),
            """
            |package community.flock.wirespec.spring.test.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record TodoDto (
            |  TodoId id,
            |  String name,
            |  Boolean done
            |) {
            |};
            |
            """.trimMargin(),
            """ 
            |package community.flock.wirespec.spring.test.model;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |public record Error (
            |  Long code,
            |  String description
            |) {
            |};
            |
            """.trimMargin(),
            """
            |package community.flock.wirespec.spring.test.endpoint;
            |
            |import community.flock.wirespec.java.Wirespec;
            |
            |import community.flock.wirespec.spring.test.model.TodoDto;
            |import community.flock.wirespec.spring.test.model.Error;
            |
            |public interface GetTodos extends Wirespec.Endpoint {
            |  class Path implements Wirespec.Path {}
            |
            |  public record Queries(
            |    java.util.Optional<Boolean> done
            |  ) implements Wirespec.Queries {}
            |
            |  class RequestHeaders implements Wirespec.Request.Headers {}
            |
            |  class Request implements Wirespec.Request<Void> {
            |    private final Path path;
            |    private final Wirespec.Method method;
            |    private final Queries queries;
            |    private final RequestHeaders headers;
            |    private final Void body;
            |    public Request(java.util.Optional<Boolean> done) {
            |      this.path = new Path();
            |      this.method = Wirespec.Method.GET;
            |      this.queries = new Queries(done);
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
            |  sealed interface Response5XX<T> extends Response<T> {}
            |  sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> {}
            |  sealed interface ResponseError extends Response<Error> {}
            |
            |  record Response200(Long total, java.util.List<TodoDto> body) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
            |    @Override public int getStatus() { return 200; }
            |    @Override public Headers getHeaders() { return new Headers(total); }
            |    @Override public java.util.List<TodoDto> getBody() { return body; }
            |    public record Headers(
            |    Long total
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
            |      var queries = new java.util.HashMap<String, java.util.List<String>>() {{
            |        request.queries.done.ifPresent(it -> put("done", serialization.serializeParam(it, Wirespec.getType(Boolean.class, false))));
            |      }};
            |      
            |      var headers = java.util.Collections.<String,java.util.List<String>>emptyMap();
            |      return new Wirespec.RawRequest(
            |        request.method.name(),
            |        java.util.List.of("api", "todos"),
            |        queries,
            |        headers,
            |        serialization.serialize(request.getBody(), Wirespec.getType(Void.class, false))
            |      );
            |    }
            |
            |    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
            |      return new Request(
            |        java.util.Optional.ofNullable(request.queries().get("done")).map(it -> serialization.<Boolean>deserializeParam(it, Wirespec.getType(Boolean.class, false)))
            |      );
            |    }
            |
            |    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
            |      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Map.ofEntries(java.util.Map.entry("total", serialization.serializeParam(r.getHeaders().total(), Wirespec.getType(Long.class, false)))), serialization.serialize(r.body, Wirespec.getType(TodoDto.class, true))); }
            |      if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(Error.class, false))); }
            |      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
            |    }
            |
            |    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
            |      switch (response.statusCode()) {
            |        case 200: return new Response200(
            |        java.util.Optional.ofNullable(response.headers().get("total")).map(it -> serialization.<Long>deserializeParam(it, Wirespec.getType(Long.class, false))).get(),
            |        serialization.deserialize(response.body(), Wirespec.getType(TodoDto.class, true))
            |      );
            |        case 500: return new Response500(
            |        serialization.deserialize(response.body(), Wirespec.getType(Error.class, false))
            |      );
            |        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
            |      }
            |    }
            |
            |    @org.springframework.web.bind.annotation.GetMapping("/api/todos")
            |    java.util.concurrent.CompletableFuture<Response<?>> getTodos(Request request);
            |
            |    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
            |      @Override public String getPathTemplate() { return "/api/todos"; }
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
            """.trimMargin(),
        )

        assertEquals(expected.toSet(), actual.toSet())
    }
}
