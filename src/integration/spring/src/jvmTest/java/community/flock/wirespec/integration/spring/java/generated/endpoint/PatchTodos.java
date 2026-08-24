package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDtoPatch;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
public interface PatchTodos extends Wirespec.Endpoint {
  public static record Path (
    String id
  ) implements Wirespec.Path {
  };
  public static record Queries () implements Wirespec.Queries {
  };
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  };
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    TodoDtoPatch body
  ) implements Wirespec.Request<TodoDtoPatch> {
    public Request(String id, TodoDtoPatch body) {
      this(new Path(id), Wirespec.Method.PATCH, new Queries(), new RequestHeaders(), body);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response5XX, ResponseTodoDto, ResponseError {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface Response5XX<T> extends Response<T> permits Response500 {}
  public sealed interface ResponseTodoDto extends Response<TodoDto> permits Response200 {}
  public sealed interface ResponseError extends Response<Error> permits Response500 {}
  public static record Response200Headers () implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    TodoDto body
  ) implements Response2XX<TodoDto>, ResponseTodoDto {
    public Response200(TodoDto body) {
      this(200, new Response200Headers(), body);
    }
  };
  public static record Response500Headers () implements Wirespec.Response.Headers {
  };
  public static record Response500 (
    Integer status,
    Response500Headers headers,
    Error body
  ) implements Response5XX<Error>, ResponseError {
    public Response500(Error body) {
      this(500, new Response500Headers(), body);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("api", "todos", serialization.<String>serializePath(request.path().id(), Wirespec.getType(String.class, null))),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      java.util.Optional.of(serialization.<TodoDtoPatch>serializeBody(request.body(), Wirespec.getType(TodoDtoPatch.class, null)))
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
      serialization.<String>deserializePath(request.path().get(2), Wirespec.getType(String.class, null)),
      request.body().map(it -> serialization.<TodoDtoPatch>deserializeBody(it, Wirespec.getType(TodoDtoPatch.class, null))).orElseThrow(() -> new IllegalStateException("body is null"))
    );
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, null)))
      );
    } else if (response instanceof Response500 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(Error.class, null)))
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200 -> {
          return new Response200(response.body().map(it -> serialization.<TodoDto>deserializeBody(it, Wirespec.getType(TodoDto.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
        }
        case 500 -> {
          return new Response500(response.body().map(it -> serialization.<Error>deserializeBody(it, Wirespec.getType(Error.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PatchMapping("/api/todos/{id}")  public java.util.concurrent.CompletableFuture<Response<?>> patchTodos(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/api/todos/{id}";
      }
      @Override
      public String getMethod() {
        return "PATCH";
      }
      @Override
      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
        return new Wirespec.ServerEdge<>() {
        @Override public Request from(Wirespec.RawRequest request) {
            return fromRawRequest(serialization, request);
        }
        @Override public Wirespec.RawResponse to(Response<?> response) {
            return toRawResponse(serialization, response);
        }
        };
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
            return toRawRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
            return fromRawResponse(serialization, response);
        }
        };
      }
    };
  }
  public interface Call extends Wirespec.Call {
    public java.util.concurrent.CompletableFuture<Response<?>> patchTodos(String id, TodoDtoPatch body);
  }
  Handler.Handlers api = new Handler.Handlers();
}
