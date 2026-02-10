package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;

public interface FindPetsByTags extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  public record Queries(
    java.util.Optional<java.util.List<String>> tags
  ) implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
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

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponseListPet extends Response<java.util.List<Pet>> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(
    int status,
    Headers headers,
    java.util.List<Pet> body
  ) implements Response2XX<java.util.List<Pet>>, ResponseListPet {
    public Response200(java.util.List<Pet> body) {
      this(200, new Headers(), body);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
  record Response400(
    int status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response400() {
      this(400, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("pet", "findByTags"),
        java.util.Map.ofEntries(java.util.Map.entry("tags", serialization.serializeParam(request.queries().tags(), Wirespec.getType(String.class, java.util.Optional.class)))),
        java.util.Collections.emptyMap(),
        java.util.Optional.empty()
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.<java.util.Optional<java.util.List<String>>>deserializeParam(request.queries().getOrDefault("tags", java.util.Collections.emptyList()), Wirespec.getType(String.class, java.util.Optional.class))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Pet.class, java.util.List.class)))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.empty()); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
          response.body().<java.util.List<Pet>>map(body -> serialization.deserializeBody(body, Wirespec.getType(Pet.class, java.util.List.class))).orElse(null)
        );
        case 400 -> new Response400();
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.GetMapping("/pet/findByTags")
    java.util.concurrent.CompletableFuture<Response<?>> findPetsByTags(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet/findByTags"; }
      @Override public String getMethod() { return "GET"; }
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
