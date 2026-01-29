package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Order;

public interface GetOrderById extends Wirespec.Endpoint {
  public record Path(
    Long orderId
  ) implements Wirespec.Path {}

  static class Queries implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(Long orderId) {
      this(new Path(orderId), Wirespec.Method.GET, new Queries(), new RequestHeaders(), null);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponseOrder extends Response<Order> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(
    int status,
    Headers headers,
    Order body
  ) implements Response2XX<Order>, ResponseOrder {
    public Response200(Order body) {
      this(200, new Headers(), body);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
  record Response400(
    int status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response400() {
      this(400, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
  record Response404(
    int status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response404() {
      this(404, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
        
  static interface Adapter extends Wirespec.Adapter<Request, Response<?>>{
    public static String pathTemplate = "/store/order/{orderId}";
    public static String method = "GET";
  static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("store", "order", serialization.serializePath(request.path().orderId(), Wirespec.getType(Long.class, null))),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      null
    );
  }

  static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
        serialization.deserializePath(request.path().get(2), Wirespec.getType(Long.class, null))
      );
  }

  static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(Order.class, null))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), null); }
      if (response instanceof Response404 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), null); }
    else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
  }

  static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserializeBody(response.body(), Wirespec.getType(Order.class, null))
      );
        case 400: return new Response400();
        case 404: return new Response404();
      default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
    }
  }
}

  interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/store/order/{orderId}")
    java.util.concurrent.CompletableFuture<Response<?>> getOrderById(Request request);

  }
}
