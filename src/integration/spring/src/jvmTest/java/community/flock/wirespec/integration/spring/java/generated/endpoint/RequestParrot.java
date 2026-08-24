package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.integration.spring.java.generated.model.Error;
public interface RequestParrot extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  };
  public static record Queries (
    java.util.Optional<String> queryParam,
    java.util.Optional<String> RanDoMQueRY
  ) implements Wirespec.Queries {
  };
  public static record RequestHeaders (
    java.util.Optional<String> xRequestID,
    java.util.Optional<String> RanDoMHeADer
  ) implements Wirespec.Request.Headers {
  };
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    RequestBodyParrot body
  ) implements Wirespec.Request<RequestBodyParrot> {
    public Request(java.util.Optional<String> queryParam, java.util.Optional<String> ranDoMQueRY, java.util.Optional<String> xRequestID, java.util.Optional<String> ranDoMHeADer, RequestBodyParrot body) {
      this(new Path(), Wirespec.Method.POST, new Queries(
        queryParam,
        ranDoMQueRY
      ), new RequestHeaders(
        xRequestID,
        ranDoMHeADer
      ), body);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response2XX, Response5XX, ResponseRequestBodyParrot, ResponseError {}
  public sealed interface Response2XX<T> extends Response<T> permits Response200 {}
  public sealed interface Response5XX<T> extends Response<T> permits Response500 {}
  public sealed interface ResponseRequestBodyParrot extends Response<RequestBodyParrot> permits Response200 {}
  public sealed interface ResponseError extends Response<Error> permits Response500 {}
  public static record Response200Headers (
    java.util.Optional<String> xRequestID,
    java.util.Optional<String> RanDoMHeADer,
    java.util.Optional<String> queryParamParrot,
    java.util.Optional<String> RanDoMQueRYParrot
  ) implements Wirespec.Response.Headers {
  };
  public static record Response200 (
    Integer status,
    Response200Headers headers,
    RequestBodyParrot body
  ) implements Response2XX<RequestBodyParrot>, ResponseRequestBodyParrot {
    public Response200(java.util.Optional<String> xRequestID, java.util.Optional<String> ranDoMHeADer, java.util.Optional<String> queryParamParrot, java.util.Optional<String> ranDoMQueRYParrot, RequestBodyParrot body) {
      this(200, new Response200Headers(
        xRequestID,
        ranDoMHeADer,
        queryParamParrot,
        ranDoMQueRYParrot
      ), body);
    }
  };
  public static record Response500Headers () implements Wirespec.Response.Headers {
  };
  public static record Response500 (
    Integer status,
    Response500Headers headers,
    Error body
  ) implements Response5XX<Error>, ResponseError {
    public Response500(Error body) {
      this(500, new Response500Headers(), body);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("api", "parrot"),
      java.util.Map.ofEntries(java.util.Map.entry("Query-Param", request.queries().queryParam().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("RanDoMQueRY", request.queries().RanDoMQueRY().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of()))),
      java.util.Map.ofEntries(java.util.Map.entry("X-Request-ID", request.headers().xRequestID().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("RanDoMHeADer", request.headers().RanDoMHeADer().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of()))),
      java.util.Optional.of(serialization.<RequestBodyParrot>serializeBody(request.body(), Wirespec.getType(RequestBodyParrot.class, null)))
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
      java.util.Optional.ofNullable(request.queries().get("Query-Param")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
      java.util.Optional.ofNullable(request.queries().get("RanDoMQueRY")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
      java.util.Optional.ofNullable(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Request-ID")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
      java.util.Optional.ofNullable(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMHeADer")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
      request.body().map(it -> serialization.<RequestBodyParrot>deserializeBody(it, Wirespec.getType(RequestBodyParrot.class, null))).orElseThrow(() -> new IllegalStateException("body is null"))
    );
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response200 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Map.ofEntries(java.util.Map.entry("X-Request-ID", r.headers().xRequestID().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("RanDoMHeADer", r.headers().RanDoMHeADer().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("Query-Param-Parrot", r.headers().queryParamParrot().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("RanDoMQueRYParrot", r.headers().RanDoMQueRYParrot().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of()))),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(RequestBodyParrot.class, null)))
      );
    } else if (response instanceof Response500 r) {
      return new Wirespec.RawResponse(
        r.status(),
        java.util.Collections.emptyMap(),
        java.util.Optional.of(serialization.serializeBody(r.body(), Wirespec.getType(Error.class, null)))
      );
    } else {
      throw new IllegalStateException(("Cannot match response with status: " + response.status()));
    }
  }
  public static Response<?> fromRawResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
    switch (response.statusCode()) {
        case 200 -> {
          return new Response200(
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Request-ID")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMHeADer")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Query-Param-Parrot")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
            java.util.Optional.ofNullable(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMQueRYParrot")).findFirst().map(java.util.Map.Entry::getValue).orElse(null)).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
            response.body().map(it -> serialization.<RequestBodyParrot>deserializeBody(it, Wirespec.getType(RequestBodyParrot.class, null))).orElseThrow(() -> new IllegalStateException("body is null"))
          );
        }
        case 500 -> {
          return new Response500(response.body().map(it -> serialization.<Error>deserializeBody(it, Wirespec.getType(Error.class, null))).orElseThrow(() -> new IllegalStateException("body is null")));
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/api/parrot")  public java.util.concurrent.CompletableFuture<Response<?>> requestParrot(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/api/parrot";
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
    public java.util.concurrent.CompletableFuture<Response<?>> requestParrot(java.util.Optional<String> queryParam, java.util.Optional<String> ranDoMQueRY, java.util.Optional<String> xRequestID, java.util.Optional<String> ranDoMHeADer, RequestBodyParrot body);
  }
  Handler.Handlers api = new Handler.Handlers();
}
