package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
public interface GetInventory extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
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
    Void body
  ) implements Wirespec.Request<Void> {
    public Request() {
      this(new Path(), Wirespec.Method.GET, new Queries(), new RequestHeaders(), null);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, ResponseMap {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface ResponseMap extends Response<java.util.Map<String, Integer>> permits Response200 {}
  public static record Response200Headers () implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    java.util.Map<String, Integer> body
  ) implements Response2XX<java.util.Map<String, Integer>>, ResponseMap {
    public Response200(java.util.Map<String, Integer> body) {
      this(200, new Response200Headers(), body);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("store", "inventory"),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      java.util.Optional.empty()
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request();
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(Integer.class, java.util.Map.class)))
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200 -> {
          return new Response200(response.body().map(it -> serialization.<java.util.Map<String, Integer>>deserializeBody(it, Wirespec.getType(Integer.class, java.util.Map.class))).orElseThrow(() -> new IllegalStateException("body is null")));
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/store/inventory")  public java.util.concurrent.CompletableFuture<Response<?>> getInventory(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/store/inventory";
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
    public java.util.concurrent.CompletableFuture<Response<?>> getInventory();
  }
  Handler.Handlers api = new Handler.Handlers();
}
