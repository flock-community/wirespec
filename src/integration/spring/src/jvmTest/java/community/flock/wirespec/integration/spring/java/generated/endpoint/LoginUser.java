package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

public interface LoginUser extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  }
  public static record Queries (
    java.util.Optional<String> username,
    java.util.Optional<String> password
  ) implements Wirespec.Queries {
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
    public Request(java.util.Optional<String> username, java.util.Optional<String> password) {
      this(new Path(), Wirespec.Method.GET, new Queries(
        username,
        password
      ), new RequestHeaders(), null);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response2XX<T> extends Response<T> {
  }
  public sealed interface Response4XX<T> extends Response<T> {
  }
  public sealed interface ResponseString extends Response<String> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
  }
  public static record Response200 (
    Integer status,
    Headers headers,
    String body
  ) implements Response2XX<String>, ResponseString {
    public Response200(java.util.Optional<Integer> XRateLimit, java.util.Optional<String> XExpiresAfter, String body) {
      this(200, new Headers(
        XRateLimit,
        XExpiresAfter
      ), body);
    }
    public static record Headers (
      java.util.Optional<Integer> XRateLimit,
      java.util.Optional<String> XExpiresAfter
    ) implements Wirespec.Response.Headers {
    }
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
  public interface Handler extends Wirespec.Handler {
    static public Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("user", "login"),
        java.util.Map.ofEntries(java.util.Map.entry("username", serialization.serializeParam(request.queries().username(), Wirespec.getType(String.class, java.util.Optional.class))), java.util.Map.entry("password", serialization.serializeParam(request.queries().password(), Wirespec.getType(String.class, java.util.Optional.class)))),
        java.util.Collections.emptyMap(),
        null
      );
    }
    static public Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializeParam(request.queries().getOrDefault("username", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
        serialization.deserializeParam(request.queries().getOrDefault("password", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class))
      );
    }
    static public Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Map.ofEntries(java.util.Map.entry("X-Rate-Limit", serialization.serializeParam(r.headers().XRateLimit(), Wirespec.getType(Integer.class, java.util.Optional.class))), java.util.Map.entry("X-Expires-After", serialization.serializeParam(r.headers().XExpiresAfter(), Wirespec.getType(String.class, java.util.Optional.class)))),
          serialization.serializeBody(r.body(), Wirespec.getType(String.class, null))
        );
      } else if (response instanceof Response400 r) {
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
            return new Response200(
              serialization.deserializeParam(response.headers().getOrDefault("X-Rate-Limit", java.util.Collections.emptyList()), Wirespec.getType(Integer.class, java.util.Optional.class)),
              serialization.deserializeParam(response.headers().getOrDefault("X-Expires-After", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)),
              serialization.deserializeBody(response.body(), Wirespec.getType(String.class, null))
            );
          }
          case 400 -> {
            return new Response400();
          }
          default -> {
            throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
          }
      }
    }
    @org.springframework.web.bind.annotation.GetMapping("/user/login")
        public java.util.concurrent.CompletableFuture<Response<?>> loginUser(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/user/login";
      }
      @Override
      public String getMethod() {
        return "GET";
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