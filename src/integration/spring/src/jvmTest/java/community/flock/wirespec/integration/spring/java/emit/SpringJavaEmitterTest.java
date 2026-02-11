package community.flock.wirespec.integration.spring.java.emit;

import arrow.core.Either;
import arrow.core.NonEmptyList;
import community.flock.wirespec.compiler.core.CompilerKt;
import community.flock.wirespec.compiler.core.FileUri;
import community.flock.wirespec.compiler.core.LanguageSpec;
import community.flock.wirespec.compiler.core.ModuleContent;
import community.flock.wirespec.compiler.core.ParseContext;
import community.flock.wirespec.compiler.core.WirespecSpec;
import community.flock.wirespec.compiler.core.emit.Emitted;
import community.flock.wirespec.compiler.core.emit.PackageName;
import community.flock.wirespec.compiler.core.parse.ast.Root;
import community.flock.wirespec.compiler.utils.Logger;
import community.flock.wirespec.compiler.utils.LoggerKt;
import org.jetbrains.annotations.NotNull;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringJavaEmitterTest {

    private Root parse(String source) {
        ParseContext context = new ParseContext() {
            @Override
            public @NotNull LanguageSpec getSpec() {
                return WirespecSpec.INSTANCE;
            }

            @Override
            public @NotNull Logger getLogger() {
                return LoggerKt.getNoLogger();
            }
        };

        final FileUri fileUri = new FileUri("");
        NonEmptyList<ModuleContent> sourceList = nonEmptyListOf(new ModuleContent(fileUri, source));

        Either result = CompilerKt.parse(context, sourceList);

        if (result instanceof Either.Right) {
            return (Root) ((Either.Right) result).getValue();
        } else {
            Either.Left left = (Either.Left) result;
            throw new RuntimeException(left.getValue().toString());
        }
    }

    @Test
    public void shouldEmitTheFullWirespecAndAddAnnotationToTheHandlerMethod() throws IOException {
        Path path = Paths.get("src/jvmTest/resources/todo.ws");
        String text = Files.readString(path);

        Root ast = parse(text);

        SpringJavaEmitter emitter = new SpringJavaEmitter(new PackageName("community.flock.wirespec.spring.test", false));

        NonEmptyList emittedNel = emitter.emit(ast, LoggerKt.getNoLogger());
        List<Emitted> emittedList = (List<Emitted>) emittedNel;

        List<String> actual = emittedList.stream()
                .map(Emitted::getResult)
                .collect(Collectors.toList());

        List<String> expected = List.of(
                """
                        package community.flock.wirespec.spring.test.endpoint;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        import community.flock.wirespec.spring.test.model.RequestBodyParrot;
                        import community.flock.wirespec.spring.test.model.Error;
                        
                        public interface RequestParrot extends Wirespec.Endpoint {
                          static class Path implements Wirespec.Path {}
                        
                          public record Queries(
                            java.util.Optional<String> QueryParam,
                            java.util.Optional<String> RanDoMQueRY
                          ) implements Wirespec.Queries {}
                        
                          public record RequestHeaders(
                            java.util.Optional<String> XRequestID,
                            java.util.Optional<String> RanDoMHeADer
                          ) implements Wirespec.Request.Headers {}
                        
                          record Request (
                            Path path,
                            Wirespec.Method method,
                            Queries queries,
                            RequestHeaders headers,
                            RequestBodyParrot body
                          ) implements Wirespec.Request<RequestBodyParrot> {
                            public Request(java.util.Optional<String> QueryParam, java.util.Optional<String> RanDoMQueRY, java.util.Optional<String> XRequestID, java.util.Optional<String> RanDoMHeADer, RequestBodyParrot body) {
                              this(new Path(), Wirespec.Method.POST, new Queries(QueryParam, RanDoMQueRY), new RequestHeaders(XRequestID, RanDoMHeADer), body);
                            }
                          }
                        
                          sealed interface Response<T> extends Wirespec.Response<T> {}
                          sealed interface Response2XX<T> extends Response<T> {}
                          sealed interface Response5XX<T> extends Response<T> {}
                          sealed interface ResponseRequestBodyParrot extends Response<RequestBodyParrot> {}
                          sealed interface ResponseError extends Response<Error> {}
                        
                          record Response200(
                            int status,
                            Headers headers,
                            RequestBodyParrot body
                          ) implements Response2XX<RequestBodyParrot>, ResponseRequestBodyParrot {
                            public Response200(java.util.Optional<String> XRequestID, java.util.Optional<String> RanDoMHeADer, java.util.Optional<String> QueryParamParrot, java.util.Optional<String> RanDoMQueRYParrot, RequestBodyParrot body) {
                              this(200, new Headers(XRequestID, RanDoMHeADer, QueryParamParrot, RanDoMQueRYParrot), body);
                            }
                            public record Headers(
                            java.util.Optional<String> XRequestID,
                            java.util.Optional<String> RanDoMHeADer,
                            java.util.Optional<String> QueryParamParrot,
                            java.util.Optional<String> RanDoMQueRYParrot
                          ) implements Wirespec.Response.Headers {}
                          }
                          record Response500(
                            int status,
                            Headers headers,
                            Error body
                          ) implements Response5XX<Error>, ResponseError {
                            public Response500(Error body) {
                              this(500, new Headers(), body);
                            }
                            static class Headers implements Wirespec.Response.Headers {}
                          }
                        
                          interface Handler extends Wirespec.Handler {
                        
                            static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
                              return new Wirespec.RawRequest(
                                request.method().name(),
                                java.util.List.of("api", "parrot"),
                                java.util.Map.ofEntries(java.util.Map.entry("Query-Param", serialization.serializeParam(request.queries().QueryParam(), Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("RanDoMQueRY", serialization.serializeParam(request.queries().RanDoMQueRY(), Wirespec.getType(String.class, java.util.Optional.class)))),
                                java.util.Map.ofEntries(java.util.Map.entry("X-Request-ID", serialization.serializeParam(request.headers().XRequestID(), Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("RanDoMHeADer", serialization.serializeParam(request.headers().RanDoMHeADer(), Wirespec.getType(String.class, java.util.Optional.class)))),
                                java.util.Optional.ofNullable(serialization.serializeBody(request.body(), Wirespec.getType(RequestBodyParrot.class, null)))
                              );
                            }

                            static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
                              return new Request(
                                serialization.<java.util.Optional<String>>deserializeParam(request.queries().getOrDefault("Query-Param", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                serialization.<java.util.Optional<String>>deserializeParam(request.queries().getOrDefault("RanDoMQueRY", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                serialization.<java.util.Optional<String>>deserializeParam(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Request-ID")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                serialization.<java.util.Optional<String>>deserializeParam(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMHeADer")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                request.body().<RequestBodyParrot>map(body -> serialization.deserializeBody(body, Wirespec.getType(RequestBodyParrot.class, null))).orElse(null)
                              );
                            }
                        
                            static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
                              if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Map.ofEntries(java.util.Map.entry("X-Request-ID", serialization.<java.util.Optional<String>>serializeParam(r.headers().XRequestID(), Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("RanDoMHeADer", serialization.<java.util.Optional<String>>serializeParam(r.headers().RanDoMHeADer(), Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("Query-Param-Parrot", serialization.<java.util.Optional<String>>serializeParam(r.headers().QueryParamParrot(), Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("RanDoMQueRYParrot", serialization.<java.util.Optional<String>>serializeParam(r.headers().RanDoMQueRYParrot(), Wirespec.getType(String.class, java.util.Optional.class)))), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(RequestBodyParrot.class, null)))); }
                              if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Error.class, null)))); }
                              else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
                            }
                        
                            static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
                              return switch (response.statusCode()) {
                                case 200 -> new Response200(
                                  serialization.<java.util.Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Request-ID")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                  serialization.<java.util.Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMHeADer")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                  serialization.<java.util.Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Query-Param-Parrot")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                  serialization.<java.util.Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMQueRYParrot")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
                                  response.body().<RequestBodyParrot>map(body -> serialization.deserializeBody(body, Wirespec.getType(RequestBodyParrot.class, null))).orElse(null)
                                );
                                case 500 -> new Response500(
                                  response.body().<Error>map(body -> serialization.deserializeBody(body, Wirespec.getType(Error.class, null))).orElse(null)
                                );
                                default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
                              };
                            }
                        
                            @org.springframework.web.bind.annotation.PostMapping("/api/parrot")
                            java.util.concurrent.CompletableFuture<Response<?>> requestParrot(Request request);
                        
                            class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
                              @Override public String getPathTemplate() { return "/api/parrot"; }
                              @Override public String getMethod() { return "POST"; }
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
                        """, """ 
                        package community.flock.wirespec.spring.test.endpoint;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        import community.flock.wirespec.spring.test.model.TodoDto;
                        import community.flock.wirespec.spring.test.model.Error;
                        
                        public interface GetTodos extends Wirespec.Endpoint {
                          static class Path implements Wirespec.Path {}
                        
                          public record Queries(
                            java.util.Optional<Boolean> done
                          ) implements Wirespec.Queries {}
                        
                          static class RequestHeaders implements Wirespec.Request.Headers {}
                        
                          record Request (
                            Path path,
                            Wirespec.Method method,
                            Queries queries,
                            RequestHeaders headers,
                            Void body
                          ) implements Wirespec.Request<Void> {
                            public Request(java.util.Optional<Boolean> done) {
                              this(new Path(), Wirespec.Method.GET, new Queries(done), new RequestHeaders(), null);
                            }
                          }
                        
                          sealed interface Response<T> extends Wirespec.Response<T> {}
                          sealed interface Response2XX<T> extends Response<T> {}
                          sealed interface Response5XX<T> extends Response<T> {}
                          sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> {}
                          sealed interface ResponseError extends Response<Error> {}
                        
                          record Response200(
                            int status,
                            Headers headers,
                            java.util.List<TodoDto> body
                          ) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
                            public Response200(Long total, java.util.List<TodoDto> body) {
                              this(200, new Headers(total), body);
                            }
                            public record Headers(
                            Long total
                          ) implements Wirespec.Response.Headers {}
                          }
                          record Response500(
                            int status,
                            Headers headers,
                            Error body
                          ) implements Response5XX<Error>, ResponseError {
                            public Response500(Error body) {
                              this(500, new Headers(), body);
                            }
                            static class Headers implements Wirespec.Response.Headers {}
                          }
                        
                          interface Handler extends Wirespec.Handler {
                        
                            static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
                              return new Wirespec.RawRequest(
                                request.method().name(),
                                java.util.List.of("api", "todos"),
                                java.util.Map.ofEntries(java.util.Map.entry("done", serialization.serializeParam(request.queries().done(), Wirespec.getType(Boolean.class, java.util.Optional.class)))),
                                java.util.Collections.emptyMap(),
                                java.util.Optional.empty()
                              );
                            }
                        
                            static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
                              return new Request(
                                serialization.<java.util.Optional<Boolean>>deserializeParam(request.queries().getOrDefault("done", java.util.Collections.emptyList()), Wirespec.getType(Boolean.class, java.util.Optional.class))
                              );
                            }
                        
                            static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
                              if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Map.ofEntries(java.util.Map.entry("total", serialization.<Long>serializeParam(r.headers().total(), Wirespec.getType(Long.class, null)))), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, java.util.List.class)))); }
                              if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Error.class, null)))); }
                              else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
                            }

                            static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
                              return switch (response.statusCode()) {
                                case 200 -> new Response200(
                                  serialization.<Long>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("total")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(Long.class, null)),
                                  response.body().<java.util.List<TodoDto>>map(body -> serialization.deserializeBody(body, Wirespec.getType(TodoDto.class, java.util.List.class))).orElse(null)
                                );
                                case 500 -> new Response500(
                                  response.body().<Error>map(body -> serialization.deserializeBody(body, Wirespec.getType(Error.class, null))).orElse(null)
                                );
                                default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
                              };
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
                        """, """ 
                        package community.flock.wirespec.spring.test.endpoint;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        import community.flock.wirespec.spring.test.model.TodoDtoPatch;
                        import community.flock.wirespec.spring.test.model.TodoDto;
                        import community.flock.wirespec.spring.test.model.Error;
                        
                        public interface PatchTodos extends Wirespec.Endpoint {
                          public record Path(
                            String id
                          ) implements Wirespec.Path {}
                        
                          static class Queries implements Wirespec.Queries {}
                        
                          static class RequestHeaders implements Wirespec.Request.Headers {}
                        
                          record Request (
                            Path path,
                            Wirespec.Method method,
                            Queries queries,
                            RequestHeaders headers,
                            TodoDtoPatch body
                          ) implements Wirespec.Request<TodoDtoPatch> {
                            public Request(String id, TodoDtoPatch body) {
                              this(new Path(id), Wirespec.Method.PATCH, new Queries(), new RequestHeaders(), body);
                            }
                          }
                        
                          sealed interface Response<T> extends Wirespec.Response<T> {}
                          sealed interface Response2XX<T> extends Response<T> {}
                          sealed interface Response5XX<T> extends Response<T> {}
                          sealed interface ResponseTodoDto extends Response<TodoDto> {}
                          sealed interface ResponseError extends Response<Error> {}
                        
                          record Response200(
                            int status,
                            Headers headers,
                            TodoDto body
                          ) implements Response2XX<TodoDto>, ResponseTodoDto {
                            public Response200(TodoDto body) {
                              this(200, new Headers(), body);
                            }
                            static class Headers implements Wirespec.Response.Headers {}
                          }
                          record Response500(
                            int status,
                            Headers headers,
                            Error body
                          ) implements Response5XX<Error>, ResponseError {
                            public Response500(Error body) {
                              this(500, new Headers(), body);
                            }
                            static class Headers implements Wirespec.Response.Headers {}
                          }
                        
                          interface Handler extends Wirespec.Handler {
                        
                            static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
                              return new Wirespec.RawRequest(
                                request.method().name(),
                                java.util.List.of("api", "todos", serialization.serializePath(request.path().id(), Wirespec.getType(String.class, null))),
                                java.util.Collections.emptyMap(),
                                java.util.Collections.emptyMap(),
                                java.util.Optional.ofNullable(serialization.serializeBody(request.body(), Wirespec.getType(TodoDtoPatch.class, null)))
                              );
                            }
                        
                            static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
                              return new Request(
                                serialization.deserializePath(request.path().get(2), Wirespec.getType(String.class, null)),
                                request.body().<TodoDtoPatch>map(body -> serialization.deserializeBody(body, Wirespec.getType(TodoDtoPatch.class, null))).orElse(null)
                              );
                            }
                        
                            static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
                              if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, null)))); }
                              if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Error.class, null)))); }
                              else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
                            }

                            static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
                              return switch (response.statusCode()) {
                                case 200 -> new Response200(
                                  response.body().<TodoDto>map(body -> serialization.deserializeBody(body, Wirespec.getType(TodoDto.class, null))).orElse(null)
                                );
                                case 500 -> new Response500(
                                  response.body().<Error>map(body -> serialization.deserializeBody(body, Wirespec.getType(Error.class, null))).orElse(null)
                                );
                                default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
                              };
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
                        """, """ 
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record Error (
                          Long code,
                          String description
                        ) {
                        };
                        """, """ 
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record RequestBodyParrot (
                          Long number,
                          String string
                        ) {
                        };
                        """, """ 
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record TodoDto (
                          TodoId id,
                          String name,
                          Boolean done
                        ) {
                        };
                        """, """ 
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record TodoDtoPatch (
                          java.util.Optional<String> name,
                          java.util.Optional<Boolean> done
                        ) {
                        };
                        """, """ 
                        package community.flock.wirespec.spring.test.model;
                        
                        import community.flock.wirespec.java.Wirespec;
                        
                        public record TodoId (String value) implements Wirespec.Refined<String> {
                          @Override
                          public String toString() { return value.toString(); }
                          public static boolean validate(TodoId record) {
                            return java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}\\\\b-[0-9a-fA-F]{4}\\\\b-[0-9a-fA-F]{4}\\\\b-[0-9a-fA-F]{4}\\\\b-[0-9a-fA-F]{12}$").matcher(record.value).find();
                          }
                          @Override
                          public String getValue() { return value; }
                        }
                        """
        );

        for (int i = 0; i < actual.size(); i++) {
            if (actual.get(i).contains("extends Wirespec.Endpoint") && !actual.get(i).contains("record Request")) {
                throw new AssertionError("ACTUAL[" + i + "] is an endpoint but does not contain 'record Request':\n" + actual.get(i));
            }
        }

        assertEquals(expected.stream().sorted().toList(), actual.stream().sorted().toList());
    }
}
