package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

public interface LogoutUser extends Wirespec.Endpoint {
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
    Void body
  ) implements Wirespec.Request<Void> {
    public Request() {
      this(new Path(), Wirespec.Method.GET, new Queries(), new RequestHeaders(), null);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface ResponsedXX<T> extends Response<T> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
  }
  public static record ResponseDefault (
    Integer status,
    Headers headers,
    Void body
  ) implements ResponsedXX<Void>, ResponseVoid {
    public ResponseDefault() {
      this(0, new Headers(), null);
    }
    public static record Headers () implements Wirespec.Response.Headers {
    }
  }
  static public Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user", "logout"),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      null
    );
  }
  static public Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request();
  }
  static public Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof ResponseDefault r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        null
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  static public Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/logout")
        public java.util.concurrent.CompletableFuture<Response<?>> logoutUser(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/user/logout";
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
        }};
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
          return toRawRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
          return fromRawResponse(serialization, response);
        }};
      }
    }
  }
}