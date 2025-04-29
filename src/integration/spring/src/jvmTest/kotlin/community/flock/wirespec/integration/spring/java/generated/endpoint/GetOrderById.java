package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Order;

public interface GetOrderById extends Wirespec.Endpoint {
  public record Path(
    Long orderId
  ) implements Wirespec.Path {}

  class Queries implements Wirespec.Queries {}

  class RequestHeaders implements Wirespec.Request.Headers {}

  class Request implements Wirespec.Request<Void> {
    private final Path path;
    private final Wirespec.Method method;
    private final Queries queries;
    private final RequestHeaders headers;
    private final Void body;
    public Request(Long orderId) {
      this.path = new Path(orderId);
      this.method = Wirespec.Method.GET;
      this.queries = new Queries();
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
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponseOrder extends Response<Order> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(Order body) implements Response2XX<Order>, ResponseOrder {
    @Override public int getStatus() { return 200; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public Order getBody() { return body; }
    class Headers implements Wirespec.Response.Headers {}
  }
  record Response400() implements Response4XX<Void>, ResponseVoid {
    @Override public int getStatus() { return 400; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public Void getBody() { return null; }
    class Headers implements Wirespec.Response.Headers {}
  }
  record Response404() implements Response4XX<Void>, ResponseVoid {
    @Override public int getStatus() { return 404; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public Void getBody() { return null; }
    class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
      var queries = java.util.Collections.<String,java.util.List<String>>emptyMap();
      var headers = java.util.Collections.<String,java.util.List<String>>emptyMap();
      return new Wirespec.RawRequest(
        request.method.name(),
        java.util.List.of("store", "order", serialization.serialize(request.path.orderId, Wirespec.getType(Long.class, false))),
        queries,
        headers,
        serialization.serialize(request.getBody(), Wirespec.getType(Void.class, false))
      );
    }

    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.<Long>deserialize(request.path().get(2), Wirespec.getType(Long.class, false))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(Order.class, false))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), null); }
      if (response instanceof Response404 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), null); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserialize(response.body(), Wirespec.getType(Order.class, false))
      );
        case 400: return new Response400();
        case 404: return new Response404();
        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      }
    }

    @org.springframework.web.bind.annotation.GetMapping("/store/order/{orderId}")
    java.util.concurrent.CompletableFuture<Response<?>> getOrderById(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/store/order/{orderId}"; }
      @Override public String getMethod() { return "GET"; }
      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization<String> serialization) {
        return new Wirespec.ServerEdge<>() {
          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
        };
      }
      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization<String> serialization) {
        return new Wirespec.ClientEdge<>() {
          @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
          @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
        };
      }
    }
  }
}
