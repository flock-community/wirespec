package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.integration.spring.java.generated.model.Error;

public interface RequestParrot extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  static class Queries implements Wirespec.Queries {}

  public record RequestHeaders(
    String XRequestID,
    String RanDoMHeADer
  ) implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    RequestBodyParrot body
  ) implements Wirespec.Request<RequestBodyParrot> {
    public Request(String XRequestID, String RanDoMHeADer, RequestBodyParrot body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(XRequestID, RanDoMHeADer), body);
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
    public Response200(String XRequestID, String RanDoMHeADer, RequestBodyParrot body) {
      this(200, new Headers(XRequestID, RanDoMHeADer), body);
    }
    public record Headers(
    String XRequestID,
    String RanDoMHeADer
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
        java.util.Collections.emptyMap(),
        java.util.Map.ofEntries(java.util.Map.entry("x-request-id", serialization.serializeParam(request.headers().XRequestID(), Wirespec.getType(String.class, null))), java.util.Map.entry("randomheader", serialization.serializeParam(request.headers().RanDoMHeADer(), Wirespec.getType(String.class, null)))),
        serialization.serializeBody(request.body(), Wirespec.getType(RequestBodyParrot.class, null))
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializeParam(request.headers().getOrDefault("x-request-id", java.util.Collections.emptyList()), Wirespec.getType(String.class, null)),
        serialization.deserializeParam(request.headers().getOrDefault("randomheader", java.util.Collections.emptyList()), Wirespec.getType(String.class, null)),
        serialization.deserializeBody(request.body(), Wirespec.getType(RequestBodyParrot.class, null))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Map.ofEntries(java.util.Map.entry("x-request-id", serialization.serializeParam(r.headers().XRequestID(), Wirespec.getType(String.class, null))), java.util.Map.entry("randomheader", serialization.serializeParam(r.headers().RanDoMHeADer(), Wirespec.getType(String.class, null)))), serialization.serializeBody(r.body, Wirespec.getType(RequestBodyParrot.class, null))); }
      if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(Error.class, null))); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserializeParam(response.headers().getOrDefault("x-request-id", java.util.Collections.emptyList()), Wirespec.getType(String.class, null)),
        serialization.deserializeParam(response.headers().getOrDefault("randomheader", java.util.Collections.emptyList()), Wirespec.getType(String.class, null)),
        serialization.deserializeBody(response.body(), Wirespec.getType(RequestBodyParrot.class, null))
      );
        case 500: return new Response500(
        serialization.deserializeBody(response.body(), Wirespec.getType(Error.class, null))
      );
        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      }
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
