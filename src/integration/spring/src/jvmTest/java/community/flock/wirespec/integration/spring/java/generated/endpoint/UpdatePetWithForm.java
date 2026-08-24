package community.flock.wirespec.integration.spring.java.generated.endpoint;
import community.flock.wirespec.java.Wirespec;
public interface UpdatePetWithForm extends Wirespec.Endpoint {
  public static record Path (
    Long petId
  ) implements Wirespec.Path {
  };
  public static record Queries (
    java.util.Optional<String> name,
    java.util.Optional<String> status
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
    public Request(Long petId, java.util.Optional<String> name, java.util.Optional<String> status) {
      this(new Path(petId), Wirespec.Method.POST, new Queries(
        name,
        status
      ), new RequestHeaders(), null);
    }
  };
  public sealed interface Response<T> extends Wirespec.Response<T> permits Response4XX, ResponseUnit {}
  public sealed interface Response4XX<T> extends Response<T> permits Response405 {}
  public sealed interface ResponseUnit extends Response<Void> permits Response405 {}
  public static record Response405Headers () implements Wirespec.Response.Headers {
  };
  public static record Response405 (
    Integer status,
    Response405Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseUnit {
    public Response405() {
      this(405, new Response405Headers(), null);
    }
  };
  public static Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("pet", serialization.<Long>serializePath(request.path().petId(), Wirespec.getType(Long.class, null))),
      java.util.Map.ofEntries(java.util.Map.entry("name", request.queries().name().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of())), java.util.Map.entry("status", request.queries().status().map(it -> serialization.<String>serializeParam(it, Wirespec.getType(String.class, null))).orElse(java.util.List.<String>of()))),
      java.util.Collections.emptyMap(),
      java.util.Optional.empty()
    );
  }
  public static Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
      serialization.<Long>deserializePath(request.path().get(1), Wirespec.getType(Long.class, null)),
      java.util.Optional.ofNullable(request.queries().get("name")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null))),
      java.util.Optional.ofNullable(request.queries().get("status")).map(it -> serialization.<String>deserializeParam(it, Wirespec.getType(String.class, null)))
    );
  }
  public static Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response405 r) {
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
        case 405 -> {
          return new Response405();
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.PostMapping("/pet/{petId}")  public java.util.concurrent.CompletableFuture<Response<?>> updatePetWithForm(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/pet/{petId}";
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
    public java.util.concurrent.CompletableFuture<Response<?>> updatePetWithForm(Long petId, java.util.Optional<String> name, java.util.Optional<String> status);
  }
  Handler.Handlers api = new Handler.Handlers();
}
