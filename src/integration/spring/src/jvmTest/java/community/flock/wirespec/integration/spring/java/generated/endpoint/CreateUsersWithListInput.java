package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.User;
public interface CreateUsersWithListInput extends Wirespec.Endpoint {
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
    java.util.List<User> body
  ) implements Wirespec.Request<java.util.List<User>> {
    public Request(java.util.List<User> body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(), body);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, ResponsedXX, ResponseUser, ResponseUnit {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface ResponsedXX<T> extends Response<T> permits ResponseDefault {}
  public sealed interface ResponseUser extends Response<User> permits Response200 {}
  public sealed interface ResponseUnit extends Response<Void> permits ResponseDefault {}
  public static record Response200Headers () implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    User body
  ) implements Response2XX<User>, ResponseUser {
    public Response200(User body) {
      this(200, new Response200Headers(), body);
    }
  };
  public static record ResponseDefaultHeaders () implements Wirespec.Response.Headers {
  };
  public static record ResponseDefault (
    Integer status,
    ResponseDefaultHeaders headers,
    Void body
  ) implements ResponsedXX<Void>, ResponseUnit {
    public ResponseDefault() {
      this(0, new ResponseDefaultHeaders(), null);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user", "createWithList"),
      java.util.Collections.emptyMap(),
      java.util.Collections.emptyMap(),
      java.util.Optional.of(serialization.<java.util.List<User>>serializeBody(request.body(), Wirespec.getType(User.class, java.util.List.class)))
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(request.body().map(it -> serialization.<java.util.List<User>>deserializeBody(it, Wirespec.getType(User.class, java.util.List.class))).orElseThrow(() -> new IllegalStateException("body is null")));
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(User.class, null)))
      );
    } else if (response instanceof ResponseDefault r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.empty()
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200 -> {
          return new Response200(response.body().map(it -> serialization.<User>deserializeBody(it, Wirespec.getType(User.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/user/createWithList")  public java.util.concurrent.CompletableFuture<Response<?>> createUsersWithListInput(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/user/createWithList";
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
    public java.util.concurrent.CompletableFuture<Response<?>> createUsersWithListInput(java.util.List<User> body);
  }
  Handler.Handlers api = new Handler.Handlers();
}
