package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.TodoDtoPatch;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.Error;

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
