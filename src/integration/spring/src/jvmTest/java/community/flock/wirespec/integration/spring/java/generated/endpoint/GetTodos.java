package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.integration.spring.java.generated.model.Error;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.java.Wirespec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;




public interface GetTodos extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  public record Queries(
    Optional<Boolean> done
  ) implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(Optional<Boolean> done) {
      this(new Path(), Wirespec.Method.GET, new Queries(done), new RequestHeaders(), null);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response5XX<T> extends Response<T> {}
  sealed interface ResponseListTodoDto extends Response<List<TodoDto>> {}
  sealed interface ResponseError extends Response<Error> {}

  record Response200(
    int status,
    Headers headers,
    List<TodoDto> body
  ) implements Response2XX<List<TodoDto>>, ResponseListTodoDto {
    public Response200(Long total, List<TodoDto> body) {
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
        List.of("api", "todos"),
        Map.ofEntries(Map.entry("done", serialization.serializeParam(request.queries().done(), Wirespec.getType(Boolean.class, Optional.class)))),
        Collections.emptyMap(),
        Optional.empty()
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.<Optional<Boolean>>deserializeParam(request.queries().getOrDefault("done", Collections.emptyList()), Wirespec.getType(Boolean.class, Optional.class))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), Map.ofEntries(Map.entry("total", serialization.<Long>serializeParam(r.headers().total(), Wirespec.getType(Long.class, null)))), Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(TodoDto.class, List.class)))); }
      if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.status(), Collections.emptyMap(), Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Error.class, null)))); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
          serialization.<Long>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("total")).findFirst().map(Map.Entry::getValue).orElse(Collections.emptyList()), Wirespec.getType(Long.class, null)),
          response.body().<List<TodoDto>>map(body -> serialization.deserializeBody(body, Wirespec.getType(TodoDto.class, List.class))).orElse(null)
        );
        case 500 -> new Response500(
          response.body().<Error>map(body -> serialization.deserializeBody(body, Wirespec.getType(Error.class, null))).orElse(null)
        );
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.GetMapping("/api/todos")
    CompletableFuture<Response<?>> getTodos(Request request);

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
