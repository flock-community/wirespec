package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
public interface LoginUser extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  };
  public static record Queries (
    java.util.Optional<String> username,
    java.util.Optional<String> password
  ) implements Wirespec.Queries {
  };
  public static record RequestHeaders () implements Wirespec.Request.Headers {
  };
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
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response4XX, ResponseString, ResponseUnit {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface Response4XX<T> extends Response<T> permits Response400 {}
  public sealed interface ResponseString extends Response<String> permits Response200 {}
  public sealed interface ResponseUnit extends Response<Void> permits Response400 {}
  public static record Response200Headers (
    java.util.Optional<Integer> xRateLimit,
    java.util.Optional<String> xExpiresAfter
  ) implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    String body
  ) implements Response2XX<String>, ResponseString {
    public Response200(java.util.Optional<Integer> xRateLimit, java.util.Optional<String> xExpiresAfter, String body) {
      this(200, new Response200Headers(
        xRateLimit,
        xExpiresAfter
      ), body);
    }
  };
  public static record Response400Headers () implements Wirespec.Response.Headers {
  };
  public static record Response400 (
    Integer status,
    Response400Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseUnit {
    public Response400() {
      this(400, new Response400Headers(), null);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("user", "login"),
      java.util.Map.ofEntries(java.util.Map.entry("username", request.queries().username().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("password", request.queries().password().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of()))),
      java.util.Collections.emptyMap(),
      java.util.Optional.empty()
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
      java.util.Optional.ofNullable(request.queries().get("username")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
      java.util.Optional.ofNullable(request.queries().get("password")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null)))
    );
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Map.ofEntries(java.util.Map.entry("X-Rate-Limit", r.headers().xRateLimit().map(it -> serialization.<Integer>serializeParam(it, Wirespec.getType(Integer.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("X-Expires-After", r.headers().xExpiresAfter().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of()))),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(String.class, null)))
      );
    } else if (response instanceof Response400 r) {
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
          return new Response200(
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Rate-Limit")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<Integer>deserializeParam(it, Wirespec.getType(Integer.class, null))),
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Expires-After")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
            response.body().map(it -> serialization.<String>deserializeBody(it, Wirespec.getType(String.class, null))).orElseThrow(() -> new IllegalStateException("body is null"))
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
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.GetMapping("/user/login")  public java.util.concurrent.CompletableFuture<Response<?>> loginUser(Request request);
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
    public java.util.concurrent.CompletableFuture<Response<?>> loginUser(java.util.Optional<String> username, java.util.Optional<String> password);
  }
  Handler.Handlers api = new Handler.Handlers();
}
