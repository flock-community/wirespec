package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

public interface DeleteOrder extends Wirespec.Endpoint {
  public static record Path (
    Long orderId
  ) implements Wirespec.Path {
  }
  public static record Queries () implements Wirespec.Queries {
  }
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  }
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(Long orderId) {
      this(new Path(orderId), Wirespec.Method.DELETE, new Queries(), new RequestHeaders(), null);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response4XX<T> extends Response<T> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
  }
  public static record Response400 (
    Integer status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response400() {
      this(400, new Headers(), null);
    }
    public static record Headers () implements Wirespec.Response.Headers {
    }
  }
  public static record Response404 (
    Integer status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response404() {
      this(404, new Headers(), null);
    }
    public static record Headers () implements Wirespec.Response.Headers {
    }
  }
  public interface Handler extends Wirespec.Handler {
    static public Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("store", "order", serialization.serializePath(request.path().orderId(), Wirespec.getType(Long.class, null))),
        java.util.Collections.emptyMap(),
        java.util.Collections.emptyMap(),
        null
      );
    }
    static public Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(serialization.deserializePath(request.path().get(2), Wirespec.getType(Long.class, null)));
    }
    static public Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response400 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Collections.emptyMap(),
          null
        );
      } else if (response instanceof Response404 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Collections.emptyMap(),
          null
        );
      } else {
        throw new IllegalStateException(("Cannot match response with status: " + response.status()));
      }
    }
    static public Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
          case 400 -> {
            return new Response400();
          }
          case 404 -> {
            return new Response404();
          }
          default -> {
            throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
          }
      }
    }
    @org.springframework.web.bind.annotation.DeleteMapping("/store/order/{orderId}")
        public java.util.concurrent.CompletableFuture<Response<?>> deleteOrder(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/store/order/{orderId}";
      }
      @Override
      public String getMethod() {
        return "DELETE";
      }
      @Override
      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
        return new Wirespec.ServerEdge<>() {
        @Override public Request from(Wirespec.RawRequest request) {
          return fromRequest(serialization, request);
        }
        @Override public Wirespec.RawResponse to(Response<?> response) {
          return toResponse(serialization, response);
        }};
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
          return toRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
          return fromResponse(serialization, response);
        }};
      }
    }
  }
}