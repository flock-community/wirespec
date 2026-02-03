package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Order;

public interface PlaceOrder extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
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
    Order body
  ) implements Wirespec.Request<Order> {
    public Request(Order body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(), body);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response2XX<T> extends Response<T> {
  }
  public sealed interface Response4XX<T> extends Response<T> {
  }
  public sealed interface ResponseOrder extends Response<Order> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
  }
  public static record Response200 (
    Integer status,
    Headers headers,
    Order body
  ) implements Response2XX<Order>, ResponseOrder {
    public Response200(Order body) {
      this(200, new Headers(), body);
    }
    public static record Headers () implements Wirespec.Response.Headers {
    }
  }
  public static record Response405 (
    Integer status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response405() {
      this(405, new Headers(), null);
    }
    public static record Headers () implements Wirespec.Response.Headers {
    }
  }
  public interface Handler extends Wirespec.Handler {
    static public Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("store", "order"),
        java.util.Collections.emptyMap(),
        java.util.Collections.emptyMap(),
        serialization.serializeBody(request.body(), Wirespec.getType(Order.class, null))
      );
    }
    static public Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(serialization.deserializeBody(request.body(), Wirespec.getType(Order.class, null)));
    }
    static public Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Collections.emptyMap(),
          serialization.serializeBody(r.body(), Wirespec.getType(Order.class, null))
        );
      } else if (response instanceof Response405 r) {
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
          case 200 -> {
            return new Response200(serialization.deserializeBody(response.body(), Wirespec.getType(Order.class, null)));
          }
          case 405 -> {
            return new Response405();
          }
          default -> {
            throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
          }
      }
    }
    @org.springframework.web.bind.annotation.PostMapping("/store/order")
        public java.util.concurrent.CompletableFuture<Response<?>> placeOrder(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/store/order";
      }
      @Override
      public String getMethod() {
        return "POST";
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