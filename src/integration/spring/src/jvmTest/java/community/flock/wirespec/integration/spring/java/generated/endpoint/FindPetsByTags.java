package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;

public interface FindPetsByTags extends Wirespec.Endpoint {
  public static record Path () implements Wirespec.Path {
  }
  public static record Queries (
    java.util.Optional<java.util.List<String>> tags
  ) implements Wirespec.Queries {
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
    public Request(java.util.Optional<java.util.List<String>> tags) {
      this(new Path(), Wirespec.Method.GET, new Queries(tags), new RequestHeaders(), null);
    }
  }
  public sealed interface Response<T> extends Wirespec.Response<T> {
  }
  public sealed interface Response2XX<T> extends Response<T> {
  }
  public sealed interface Response4XX<T> extends Response<T> {
  }
  public sealed interface ResponseListPet extends Response<java.util.List<Pet>> {
  }
  public sealed interface ResponseVoid extends Response<Void> {
  }
  public static record Response200 (
    Integer status,
    Headers headers,
    java.util.List<Pet> body
  ) implements Response2XX<java.util.List<Pet>>, ResponseListPet {
    public Response200(java.util.List<Pet> body) {
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
  public interface Handler extends Wirespec.Handler {
    static public Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("pet", "findByTags"),
        java.util.Map.ofEntries(java.util.Map.entry("tags", serialization.serializeParam(request.queries().tags(), Wirespec.getType(String.class, java.util.Optional.class)))),
        java.util.Collections.emptyMap(),
        null
      );
    }
    static public Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(serialization.deserializeParam(request.queries().getOrDefault("tags", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class)));
    }
    static public Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Collections.emptyMap(),
          serialization.serializeBody(r.body(), Wirespec.getType(Pet.class, java.util.List.class))
        );
      } else if (response instanceof Response400 r) {
        return new Wirespec.RawResponse(
          r.status(),
          java.util.Collections.emptyMap(),
          null
        );
      } else {
        throw new IllegalStateException(("Cannot match response with status: " + response.status()));
      }
    }
    static public Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      switch (response.statusCode()) {
          case 200 -> {
            return new Response200(serialization.deserializeBody(response.body(), Wirespec.getType(Pet.class, java.util.List.class)));
          }
          case 400 -> {
            return new Response400();
          }
          default -> {
            throw new IllegalStateException(("Cannot match response with status: " + response.statusCode()));
          }
      }
    }
    @org.springframework.web.bind.annotation.GetMapping("/pet/findByTags")
        public java.util.concurrent.CompletableFuture<Response<?>> findPetsByTags(Request request);
    public static record Handlers () implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override
      public String getPathTemplate() {
        return "/pet/findByTags";
      }
      @Override
      public String getMethod() {
        return "GET";
      }
      @Override
      public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization serialization) {
        return new Wirespec.ServerEdge<>() {
        @Override public Request from(Wirespec.RawRequest request) {
          return fromRequest(serialization, request);
        }
        @Override public Wirespec.RawResponse to(Response<?> response) {
          return toResponse(serialization, response);
        }};
      }
      @Override
      public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization serialization) {
        return new Wirespec.ClientEdge<>() {
        @Override public Wirespec.RawRequest to(Request request) {
          return toRequest(serialization, request);
        }
        @Override public Response<?> from(Wirespec.RawResponse response) {
          return fromResponse(serialization, response);
        }};
      }
    }
  }
}