package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

public interface DeletePet extends Wirespec.Endpoint {
  public static record Path (
    Long petId
  ) implements Wirespec.Path {
  }
  public static record Queries () implements Wirespec.Queries {
  }
  public static record RequestHeaders (
    java.util.Optional<String> api_key
  ) implements Wirespec.Request.Headers {
  }
  public static record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(Long petId, java.util.Optional<String> api_key) {
      this(new Path(petId), Wirespec.Method.DELETE, new Queries(), new RequestHeaders(api_key), null);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response4XX<T> extends Response<T> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
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
  static public Wirespec.RawRequest toRawRequest(Wirespec.Serializer serialization, Request request) {
    return new Wirespec.RawRequest(
      request.method().name(),
      java.util.List.of("pet", serialization.serializePath(request.path().petId(), Wirespec.getType(Long.class, null))),
      java.util.Collections.emptyMap(),
      java.util.Map.ofEntries(java.util.Map.entry("api_key", serialization.serializeParam(request.headers().api_key(), Wirespec.getType(String.class, java.util.Optional.class)))),
      null
    );
  }
  static public Request fromRawRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
    return new Request(
      serialization.deserializePath(request.path().get(1), Wirespec.getType(Long.class, null)),
      serialization.deserializeParam(request.headers().getOrDefault("api_key", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class))
    );
  }
  static public Wirespec.RawResponse toRawResponse(Wirespec.Serializer serialization, Response<?> response) {
    if (response instanceof Response400 r) {
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
        case 400 -> {
          return new Response400();
        }
        default -> {
          throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
        }
    }
  }
  public interface Handler extends Wirespec.Handler {
    @org.springframework.web.bind.annotation.DeleteMapping("/pet/{petId}")
        public java.util.concurrent.CompletableFuture<Response<?>> deletePet(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/pet/{petId}";
      }
      @Override
      public String getMethod() {
        return "DELETE";
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