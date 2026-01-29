package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;



public interface LoginUser extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  public record Queries(
    java.util.Optional<String> username,
    java.util.Optional<String> password
  ) implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(java.util.Optional<String> username, java.util.Optional<String> password) {
      this(new Path(), Wirespec.Method.GET, new Queries(username, password), new RequestHeaders(), null);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponseString extends Response<String> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(
    int status,
    Headers headers,
    String body
  ) implements Response2XX<String>, ResponseString {
    public Response200(java.util.Optional<Integer> XRateLimit, java.util.Optional<String> XExpiresAfter, String body) {
      this(200, new Headers(XRateLimit, XExpiresAfter), body);
    }
    public record Headers(
    java.util.Optional<Integer> XRateLimit,
    java.util.Optional<String> XExpiresAfter
  ) implements Wirespec.Response.Headers {}
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
        
  static interface Adapter extends Wirespec.Adapter<Request, Response<?>>{
    public static String pathTemplate = "/user/login";
    public static String method = "GET";
  static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user", "login"),
      java.util.Map.ofEntries(java.util.Map.entry("username", serialization.serializeParam(request.queries().username(), Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("password", serialization.serializeParam(request.queries().password(), Wirespec.getType(String.class, java.util.Optional.class)))),
      java.util.Collections.emptyMap(),
      null
    );
  }

  static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
        serialization.deserializeParam(request.queries().getOrDefault("username", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
        serialization.deserializeParam(request.queries().getOrDefault("password", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class))
      );
  }

  static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Map.ofEntries(java.util.Map.entry("X-Rate-Limit", serialization.serializeParam(r.headers().XRateLimit(), Wirespec.getType(Integer.class, java.util.Optional.class))), java.util.Map.entry("X-Expires-After", serialization.serializeParam(r.headers().XExpiresAfter(), Wirespec.getType(String.class, java.util.Optional.class)))), serialization.serializeBody(r.body, Wirespec.getType(String.class, null))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), null); }
    else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
  }

  static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
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
}

  interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/login")
    java.util.concurrent.CompletableFuture<Response<?>> loginUser(Request request);

  }
}
