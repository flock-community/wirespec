package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.User;
public interface CreateUser extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  };
  public static record Queries () implements Wirespec.Queries {
  };
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  };
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    User body
  ) implements Wirespec.Request<User> {
    public Request(User body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(), body);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits ResponsedXX, ResponseUser {}
  public sealed interface ResponsedXX<T> extends Response<T> permits ResponseDefault {}
  public sealed interface ResponseUser extends Response<User> permits ResponseDefault {}
  public static record ResponseDefaultHeaders () implements Wirespec.Response.Headers {
  };
  public static record ResponseDefault (
    Integer status,
    ResponseDefaultHeaders headers,
    User body
  ) implements ResponsedXX<User>, ResponseUser {
    public ResponseDefault(User body) {
      this(0, new ResponseDefaultHeaders(), body);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user"),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      java.util.Optional.of(serialization.<User>serializeBody(request.body(), Wirespec.getType(User.class, null)))
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(request.body().map(it -> serialization.<User>deserializeBody(it, Wirespec.getType(User.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof ResponseDefault r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(User.class, null)))
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/user")  public java.util.concurrent.CompletableFuture<Response<?>> createUser(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/user";
      }
      @Override
      public String getMethod() {
        return "POST";
      }
      @Override
      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
        return new Wirespec.ServerEdge<>() {
        @Override public Request from(Wirespec.RawRequest request) {
            return fromRawRequest(serialization, request);
        }
        @Override public Wirespec.RawResponse to(Response<?> response) {
            return toRawResponse(serialization, response);
        }
        };
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
            return toRawRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
            return fromRawResponse(serialization, response);
        }
        };
      }
    };
  }
  public interface Call extends Wirespec.Call {
    public java.util.concurrent.CompletableFuture<Response<?>> createUser(User body);
  }
  Handler.Handlers api = new Handler.Handlers();
}
