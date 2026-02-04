package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.User;

public interface GetUserByName extends Wirespec.Endpoint {
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
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(String username) {
      this(new Path(username), Wirespec.Method.GET, new Queries(), new RequestHeaders(), null);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response2XX<T> extends Response<T> {
  }
  public sealed interface Response4XX<T> extends Response<T> {
  }
  public sealed interface ResponseUser extends Response<User> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
  }
  public static record Response200 (
    Integer status,
    Headers headers,
    User body
  ) implements Response2XX<User>, ResponseUser {
    public Response200(User body) {
      this(200, new Headers(), body);
    }
    public static record Headers () implements Wirespec.Response.Headers {
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
  static public Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user", serialization.serializePath(request.path().username(), Wirespec.getType(String.class, null))),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      null
    );
  }
  static public Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(serialization.deserializePath(request.path().get(1), Wirespec.getType(String.class, null)));
  }
  static public Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        serialization.serializeBody(r.body(), Wirespec.getType(User.class, null))
      );
    } else if (response instanceof Response400 r) {
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
  static public Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200 -> {
          return new Response200(serialization.deserializeBody(response.body(), Wirespec.getType(User.class, null)));
        }
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
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/{username}")
        public java.util.concurrent.CompletableFuture<Response<?>> getUserByName(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/user/{username}";
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