package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.integration.spring.java.generated.model.Error;
import community.flock.wirespec.integration.spring.java.generated.model.RequestBodyParrot;
import community.flock.wirespec.java.Wirespec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;




public interface RequestParrot extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  public record Queries(
    Optional<String> QueryParam,
    Optional<String> RanDoMQueRY
  ) implements Wirespec.Queries {}

  public record RequestHeaders(
    Optional<String> XRequestID,
    Optional<String> RanDoMHeADer
  ) implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    RequestBodyParrot body
  ) implements Wirespec.Request<RequestBodyParrot> {
    public Request(Optional<String> QueryParam, Optional<String> RanDoMQueRY, Optional<String> XRequestID, Optional<String> RanDoMHeADer, RequestBodyParrot body) {
      this(new Path(), Wirespec.Method.POST, new Queries(QueryParam, RanDoMQueRY), new RequestHeaders(XRequestID, RanDoMHeADer), body);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response5XX<T> extends Response<T> {}
  sealed interface ResponseRequestBodyParrot extends Response<RequestBodyParrot> {}
  sealed interface ResponseError extends Response<Error> {}

  record Response200(
    int status,
    Headers headers,
    RequestBodyParrot body
  ) implements Response2XX<RequestBodyParrot>, ResponseRequestBodyParrot {
    public Response200(Optional<String> XRequestID, Optional<String> RanDoMHeADer, Optional<String> QueryParamParrot, Optional<String> RanDoMQueRYParrot, RequestBodyParrot body) {
      this(200, new Headers(XRequestID, RanDoMHeADer, QueryParamParrot, RanDoMQueRYParrot), body);
    }
    public record Headers(
    Optional<String> XRequestID,
    Optional<String> RanDoMHeADer,
    Optional<String> QueryParamParrot,
    Optional<String> RanDoMQueRYParrot
  ) implements Wirespec.Response.Headers {}
  }
  record Response500(
    int status,
    Headers headers,
    Error body
  ) implements Response5XX<Error>, ResponseError {
    public Response500(Error body) {
      this(500, new Headers(), body);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        List.of("api", "parrot"),
        Map.ofEntries(Map.entry("Query-Param", serialization.serializeParam(request.queries().QueryParam(), Wirespec.getType(String.class, Optional.class))), Map.entry("RanDoMQueRY", serialization.serializeParam(request.queries().RanDoMQueRY(), Wirespec.getType(String.class, Optional.class)))),
        Map.ofEntries(Map.entry("X-Request-ID", serialization.serializeParam(request.headers().XRequestID(), Wirespec.getType(String.class, Optional.class))), Map.entry("RanDoMHeADer", serialization.serializeParam(request.headers().RanDoMHeADer(), Wirespec.getType(String.class, Optional.class)))),
        Optional.ofNullable(serialization.serializeBody(request.body(), Wirespec.getType(RequestBodyParrot.class, null)))
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.<Optional<String>>deserializeParam(request.queries().getOrDefault("Query-Param", Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
        serialization.<Optional<String>>deserializeParam(request.queries().getOrDefault("RanDoMQueRY", Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
        serialization.<Optional<String>>deserializeParam(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Request-ID")).findFirst().map(Map.Entry::getValue).orElse(Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
        serialization.<Optional<String>>deserializeParam(request.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMHeADer")).findFirst().map(Map.Entry::getValue).orElse(Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
        request.body().<RequestBodyParrot>map(body -> serialization.deserializeBody(body, Wirespec.getType(RequestBodyParrot.class, null))).orElse(null)
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), Map.ofEntries(Map.entry("X-Request-ID", serialization.<Optional<String>>serializeParam(r.headers().XRequestID(), Wirespec.getType(String.class, Optional.class))), Map.entry("RanDoMHeADer", serialization.<Optional<String>>serializeParam(r.headers().RanDoMHeADer(), Wirespec.getType(String.class, Optional.class))), Map.entry("Query-Param-Parrot", serialization.<Optional<String>>serializeParam(r.headers().QueryParamParrot(), Wirespec.getType(String.class, Optional.class))), Map.entry("RanDoMQueRYParrot", serialization.<Optional<String>>serializeParam(r.headers().RanDoMQueRYParrot(), Wirespec.getType(String.class, Optional.class)))), Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(RequestBodyParrot.class, null)))); }
      if (response instanceof Response500 r) { return new Wirespec.RawResponse(r.status(), Collections.emptyMap(), Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Error.class, null)))); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
          serialization.<Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Request-ID")).findFirst().map(Map.Entry::getValue).orElse(Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
          serialization.<Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMHeADer")).findFirst().map(Map.Entry::getValue).orElse(Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
          serialization.<Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Query-Param-Parrot")).findFirst().map(Map.Entry::getValue).orElse(Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
          serialization.<Optional<String>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("RanDoMQueRYParrot")).findFirst().map(Map.Entry::getValue).orElse(Collections.emptyList()), Wirespec.getType(String.class, Optional.class)),
          response.body().<RequestBodyParrot>map(body -> serialization.deserializeBody(body, Wirespec.getType(RequestBodyParrot.class, null))).orElse(null)
        );
        case 500 -> new Response500(
          response.body().<Error>map(body -> serialization.deserializeBody(body, Wirespec.getType(Error.class, null))).orElse(null)
        );
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/parrot")
    CompletableFuture<Response<?>> requestParrot(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/api/parrot"; }
      @Override public String getMethod() { return "POST"; }
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
