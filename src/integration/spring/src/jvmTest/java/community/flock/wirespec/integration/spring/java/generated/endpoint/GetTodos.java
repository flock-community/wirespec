package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.TodoDto;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
public interface GetTodos extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  };
  public static record Queries (
    java.util.Optional<Boolean> done
  ) implements Wirespec.Queries {
  };
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  };
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(java.util.Optional<Boolean> done) {
      this(new Path(), Wirespec.Method.GET, new Queries(done), new RequestHeaders(), null);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response5XX, ResponseListTodoDto, ResponseError {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface Response5XX<T> extends Response<T> permits Response500 {}
  public sealed interface ResponseListTodoDto extends Response<java.util.List<TodoDto>> permits Response200 {}
  public sealed interface ResponseError extends Response<Error> permits Response500 {}
  public static record Response200Headers (
    Long total
  ) implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    java.util.List<TodoDto> body
  ) implements Response2XX<java.util.List<TodoDto>>, ResponseListTodoDto {
    public Response200(Long total, java.util.List<TodoDto> body) {
      this(200, new Response200Headers(total), body);
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
      java.util.List.of("api", "todos"),
      java.util.Map.ofEntries(java.util.Map.entry("done", request.queries().done().map(it -> serialization.<Boolean>serializeParam(it, Wirespec.getType(Boolean.class, null))).orElse(java.util.List.<String>of()))),
      java.util.Collections.emptyMap(),
      java.util.Optional.empty()
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(java.util.Optional.ofNullable(request.queries().get("done")).map(it -> serialization.<Boolean>deserializeParam(it, Wirespec.getType(Boolean.class, null))));
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Map.ofEntries(java.util.Map.entry("total", serialization.<Long>serializeParam(r.headers().total(), Wirespec.getType(Long.class, null)))),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(TodoDto.class, java.util.List.class)))
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
          return new Response200(
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("total")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<Long>deserializeParam(it, Wirespec.getType(Long.class, null))).orElseThrow(() -> new IllegalStateException("Param total cannot be null")),
            response.body().map(it -> serialization.<java.util.List<TodoDto>>deserializeBody(it, Wirespec.getType(TodoDto.class, java.util.List.class))).orElseThrow(() -> new IllegalStateException("body is null"))
          );
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
    @org.springframework.web.bind.annotation.GetMapping("/api/todos")  public java.util.concurrent.CompletableFuture<Response<?>> getTodos(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/api/todos";
      }
      @Override
      public String getMethod() {
        return "GET";
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
    public java.util.concurrent.CompletableFuture<Response<?>> getTodos(java.util.Optional<Boolean> done);
  }
  Handler.Handlers api = new Handler.Handlers();
}
