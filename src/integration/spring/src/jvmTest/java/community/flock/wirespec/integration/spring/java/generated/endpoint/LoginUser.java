package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;



public interface LoginUser extends Wirespec.Endpoint {
  class Path implements Wirespec.Path {}

  public record Queries(
    java.util.Optional<String> username,
    java.util.Optional<String> password
  ) implements Wirespec.Queries {}

  class RequestHeaders implements Wirespec.Request.Headers {}

  class Request implements Wirespec.Request<Void> {
    private final Path path;
    private final Wirespec.Method method;
    private final Queries queries;
    private final RequestHeaders headers;
    private final Void body;
    public Request(java.util.Optional<String> username, java.util.Optional<String> password) {
      this.path = new Path();
      this.method = Wirespec.Method.GET;
      this.queries = new Queries(username, password);
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
  sealed interface ResponseString extends Response<String> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(java.util.Optional<Integer> XRateLimit, java.util.Optional<String> XExpiresAfter, String body) implements Response2XX<String>, ResponseString {
    @Override public int getStatus() { return 200; }
    @Override public Headers getHeaders() { return new Headers(XRateLimit, XExpiresAfter); }
    @Override public String getBody() { return body; }
    public record Headers(
    java.util.Optional<Integer> XRateLimit,
    java.util.Optional<String> XExpiresAfter
  ) implements Wirespec.Response.Headers {}
  }
  record Response400() implements Response4XX<Void>, ResponseVoid {
    @Override public int getStatus() { return 400; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public Void getBody() { return null; }
    class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method.name(),
        java.util.List.of("user", "login"),
        java.util.Map.ofEntries(java.util.Map.entry("username", serialization.serializeParam(request.queries.username, Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("password", serialization.serializeParam(request.queries.password, Wirespec.getType(String.class, java.util.Optional.class)))),
        java.util.Collections.emptyMap(),
        null
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializeParam(request.queries().getOrDefault("username", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
        serialization.deserializeParam(request.queries().getOrDefault("password", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Map.ofEntries(java.util.Map.entry("X-Rate-Limit", serialization.serializeParam(r.getHeaders().XRateLimit(), Wirespec.getType(Integer.class, java.util.Optional.class))), java.util.Map.entry("X-Expires-After", serialization.serializeParam(r.getHeaders().XExpiresAfter(), Wirespec.getType(String.class, java.util.Optional.class)))), serialization.serializeBody(r.body, Wirespec.getType(String.class, null))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), null); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
        case 200: return new Response200(
        serialization.deserializeParam(response.headers().getOrDefault("X-Rate-Limit", java.util.Collections.emptyList()), Wirespec.getType(Integer.class, java.util.Optional.class)),
        serialization.deserializeParam(response.headers().getOrDefault("X-Expires-After", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
        serialization.deserializeBody(response.body(), Wirespec.getType(String.class, null))
      );
        case 400: return new Response400();
        default: throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      }
    }

    @org.springframework.web.bind.annotation.GetMapping("/user/login")
    java.util.concurrent.CompletableFuture<Response<?>> loginUser(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/user/login"; }
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
