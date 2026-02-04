package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.User;

public interface UpdateUser extends Wirespec.Endpoint {
  public static record Path (
    String username
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
    User body
  ) implements Wirespec.Request<User> {
    public Request(String username, User body) {
      this(new Path(username), Wirespec.Method.PUT, new Queries(), new RequestHeaders(), body);
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
      java.util.List.of("user", serialization.serializePath(request.path().username(), Wirespec.getType(String.class, null))),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      serialization.serializeBody(request.body(), Wirespec.getType(User.class, null))
    );
  }
  static public Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
      serialization.deserializePath(request.path().get(1), Wirespec.getType(String.class, null)),
      serialization.deserializeBody(request.body(), Wirespec.getType(User.class, null))
    );
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
    @org.springframework.web.bind.annotation.PutMapping("/user/{username}")
        public java.util.concurrent.CompletableFuture<Response<?>> updateUser(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/user/{username}";
      }
      @Override
      public String getMethod() {
        return "PUT";
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