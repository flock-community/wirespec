package community.flock.wirespec.integration.spring.java.emit;

import arrow.core.Either;
import arrow.core.NonEmptyList;
import community.flock.wirespec.compiler.core.*;
import community.flock.wirespec.compiler.core.emit.Emitted;
import community.flock.wirespec.compiler.core.emit.PackageName;
import community.flock.wirespec.compiler.core.parse.AST;
import community.flock.wirespec.compiler.utils.Logger;
import community.flock.wirespec.compiler.utils.LoggerKt;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static arrow.core.NonEmptyListKt.nonEmptyListOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpringJavaEmitterTest {

    private AST parse(String source) {
        ParseContext context = new ParseContext() {
            @Override
            public LanguageSpec getSpec() {
                return WirespecSpec.INSTANCE;
            }

            @Override
            public Logger getLogger() {
                return LoggerKt.getNoLogger();
            }
        };

        final FileUri fileUri = new FileUri("");
        final NonEmptyList sourceList = nonEmptyListOf(new ModuleContent(fileUri, source));

        Either result = CompilerKt.parse(context, sourceList);

        if (result instanceof Either.Right) {
            return (AST) ((Either.Right) result).getValue();
        } else {
            Either.Left left = (Either.Left) result;
            throw new RuntimeException(left.getValue().toString());
        }
    }

    @Test
    public void shouldEmitTheFullWirespecAndAddAnnotationToTheHandlerMethod() throws IOException {
        Path path = Paths.get("src/jvmTest/resources/todo.ws");
        String text = Files.readString(path);

        AST ast = parse(text);

        SpringJavaEmitter emitter = new SpringJavaEmitter(new PackageName("community.flock.wirespec.spring.test", false));

        NonEmptyList emittedNel = emitter.emit(ast, LoggerKt.getNoLogger());
        List<Emitted> emittedList = (List<Emitted>) emittedNel;

        List<String> actual = emittedList.stream()
                .map(Emitted::getResult)
                .collect(Collectors.toList());

        List<String> expected = List.of(
                """
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record TodoId (String value) implements Wirespec.Refined {
                          @Override
                          public String toString() { return value; }
                          public static boolean validate(TodoId record) {
                            return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\\\b-[0-9a-fA-F]{4}\\\\b-[0-9a-fA-F]{4}\\\\b-[0-9a-fA-F]{4}\\\\b-[0-9a-fA-F]{12}$").matcher(record.value).find();
                          }
                          @Override
                          public String getValue() { return value; }
                        }
                        """,
                """
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record TodoDto (
                          TodoId id,
                          String name,
                          Boolean done
                        ) {
                        };
                        """,
                """ 
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record TodoDtoPatch (
                          java.util.Optional<String> name,
                          java.util.Optional<Boolean> done
                        ) {
                        };
                        """,
                """
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record Error (
                          Long code,
                          String description
                        ) {
                        };
                        """,
                """ 
                        package community.flock.wirespec.spring.test.endpoint;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        import community.flock.wirespec.spring.test.model.TodoDto;
                        import community.flock.wirespec.spring.test.model.Error;
                        
                        public interface GetTodos extends Wirespec.Endpoint {
                          class Path implements Wirespec.Path {}
                        
                          public record Queries(
                            java.util.Optional<Boolean> done
                          ) implements Wirespec.Queries {}
                        
                          class RequestHeaders implements Wirespec.Request.Headers {}
                        
                          class Request implements Wirespec.Request<Void> {
                            private final Path path;
                            private final Wirespec.Method method;
                            private final Queries queries;
                            private final RequestHeaders headers;
                            private final Void body;
                            public Request(java.util.Optional<Boolean> done) {
                              this.path = new Path();
                              this.method = Wirespec.Method.GET;
                              this.queries = new Queries(done);
                              this.headers = new RequestHeaders();
                              this.body = null;
                            }
                            @Override public Path getPath() { return path; }
                            @Override public Wirespec.Method getMethod() { return method; }
                            @Override public Queries getQueries() { return queries; }
                            @Override public RequestHeaders getHeaders() { return headers; }
                            @Override public Void getBody() { return body; }
                          }
                        
                          sealed interface Response<T> extends Wirespec.Response<T> {}
                          sealed interface Response2XX<T> extends Response<T> {}
                          sealed interface Response5XX<T> extends Response<T> {}
                          sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> {}
                          sealed interface ResponseError extends Response<Error> {}
                        
                          record Response200(Long total, java.util.List<TodoDto> body) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
                            @Override public int getStatus() { return 200; }
                            @Override public Headers getHeaders() { return new Headers(total); }
                            @Override public java.util.List<TodoDto> getBody() { return body; }
                            public record Headers(
                            Long total
                          ) implements Wirespec.Response.Headers {}
                          }
                          record Response500(Error body) implements Response5XX<Error>, ResponseError {
                            @Override public int getStatus() { return 500; }
                            @Override public Headers getHeaders() { return new Headers(); }
                            @Override public Error getBody() { return body; }
                            class Headers implements Wirespec.Response.Headers {}
                          }
                        
                          interface Handler extends Wirespec.Handler {
                        
                            static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
                              return new Wirespec.RawRequest(
                                request.method.name(),
                                java.util.List.of("api", "todos"),
                                java.util.Map.ofEntries(java.util.Map.entry("done", serialization.serializeParam(request.queries.done, Wirespec.getType(Boolean.class, java.util.Optional.class)))),
                                java.util.Collections.emptyMap(),
                                null
                              );
                            }
                        
                            static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
                              return new Request(
                                serialization.deserializeParam(request.queries().getOrDefault("done", java.util.Collections.emptyList()), Wirespec.getType(Boolean.class, java.util.Optional.class))
                              );
                            }
                        
                            static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
                              if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Map.ofEntries(java.util.Map.entry("total", serialization.serializeParam(r.getHeaders().total(), Wirespec.getType(Long.class, null)))), serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, java.util.List.class))); }
                              if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(Error.class, null))); }
                              else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
                            }
                        
                            static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
                              switch (response.statusCode()) {
                                case 200: return new Response200(
                                serialization.deserializeParam(response.headers().getOrDefault("total", java.util.Collections.emptyList()), Wirespec.getType(Long.class, null)),
                                serialization.deserializeBody(response.body(), Wirespec.getType(TodoDto.class, java.util.List.class))
                              );
                                case 500: return new Response500(
                                serialization.deserializeBody(response.body(), Wirespec.getType(Error.class, null))
                              );
                                default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
                              }
                            }
                        
                            @org.springframework.web.bind.annotation.GetMapping("/api/todos")
                            java.util.concurrent.CompletableFuture<Response<?>> getTodos(Request request);
                        
                            class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
                              @Override public String getPathTemplate() { return "/api/todos"; }
                              @Override public String getMethod() { return "GET"; }
                              @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
                                return new Wirespec.ServerEdge<>() {
                                  @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
                                  @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
                                };
                              }
                              @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
                                return new Wirespec.ClientEdge<>() {
                                  @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
                                  @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
                                };
                              }
                            }
                          }
                        }
                        """,
                """
                        package community.flock.wirespec.spring.test.endpoint;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        import community.flock.wirespec.spring.test.model.TodoDtoPatch;
                        import community.flock.wirespec.spring.test.model.TodoDto;
                        import community.flock.wirespec.spring.test.model.Error;
                        
                        public interface PatchTodos extends Wirespec.Endpoint {
                          public record Path(
                            String id
                          ) implements Wirespec.Path {}
                        
                          class Queries implements Wirespec.Queries {}
                        
                          class RequestHeaders implements Wirespec.Request.Headers {}
                        
                          class Request implements Wirespec.Request<TodoDtoPatch> {
                            private final Path path;
                            private final Wirespec.Method method;
                            private final Queries queries;
                            private final RequestHeaders headers;
                            private final TodoDtoPatch body;
                            public Request(String id, TodoDtoPatch body) {
                              this.path = new Path(id);
                              this.method = Wirespec.Method.PATCH;
                              this.queries = new Queries();
                              this.headers = new RequestHeaders();
                              this.body = body;
                            }
                            @Override public Path getPath() { return path; }
                            @Override public Wirespec.Method getMethod() { return method; }
                            @Override public Queries getQueries() { return queries; }
                            @Override public RequestHeaders getHeaders() { return headers; }
                            @Override public TodoDtoPatch getBody() { return body; }
                          }
                        
                          sealed interface Response<T> extends Wirespec.Response<T> {}
                          sealed interface Response2XX<T> extends Response<T> {}
                          sealed interface Response5XX<T> extends Response<T> {}
                          sealed interface ResponseTodoDto extends Response<TodoDto> {}
                          sealed interface ResponseError extends Response<Error> {}
                        
                          record Response200(TodoDto body) implements Response2XX<TodoDto>, ResponseTodoDto {
                            @Override public int getStatus() { return 200; }
                            @Override public Headers getHeaders() { return new Headers(); }
                            @Override public TodoDto getBody() { return body; }
                            class Headers implements Wirespec.Response.Headers {}
                          }
                          record Response500(Error body) implements Response5XX<Error>, ResponseError {
                            @Override public int getStatus() { return 500; }
                            @Override public Headers getHeaders() { return new Headers(); }
                            @Override public Error getBody() { return body; }
                            class Headers implements Wirespec.Response.Headers {}
                          }
                        
                          interface Handler extends Wirespec.Handler {
                        
                            static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
                              return new Wirespec.RawRequest(
                                request.method.name(),
                                java.util.List.of("api", "todos", serialization.serializePath(request.path.id, Wirespec.getType(String.class, null))),
                                java.util.Collections.emptyMap(),
                                java.util.Collections.emptyMap(),
                                serialization.serializeBody(request.getBody(), Wirespec.getType(TodoDtoPatch.class, null))
                              );
                            }
                        
                            static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
                              return new Request(
                                serialization.deserializePath(request.path().get(2), Wirespec.getType(String.class, null)),
                                serialization.deserializeBody(request.body(), Wirespec.getType(TodoDtoPatch.class, null))
                              );
                            }
                        
                            static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
                              if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, null))); }
                              if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(Error.class, null))); }
                              else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
                            }
                        
                            static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
                              switch (response.statusCode()) {
                                case 200: return new Response200(
                                serialization.deserializeBody(response.body(), Wirespec.getType(TodoDto.class, null))
                              );
                                case 500: return new Response500(
                                serialization.deserializeBody(response.body(), Wirespec.getType(Error.class, null))
                              );
                                default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
                              }
                            }
                        
                            @org.springframework.web.bind.annotation.RequestMapping(value="/api/todos/{id}", method = org.springframework.web.bind.annotation.RequestMethod.PATCH)
                            java.util.concurrent.CompletableFuture<Response<?>> patchTodos(Request request);
                        
                            class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
                              @Override public String getPathTemplate() { return "/api/todos/{id}"; }
                              @Override public String getMethod() { return "PATCH"; }
                              @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
                                return new Wirespec.ServerEdge<>() {
                                  @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
                                  @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
                                };
                              }
                              @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
                                return new Wirespec.ClientEdge<>() {
                                  @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
                                  @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
                                };
                              }
                            }
                          }
                        }
                        """
        );

        assertEquals(Set.copyOf(expected), Set.copyOf(actual));
    }
}
