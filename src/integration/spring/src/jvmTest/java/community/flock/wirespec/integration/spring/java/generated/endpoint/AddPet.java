package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;

public interface AddPet extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  static class Queries implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Pet body
  ) implements Wirespec.Request<Pet> {
    public Request(Pet body) {
      this(new Path(), Wirespec.Method.POST, new Queries(), new RequestHeaders(), body);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponsePet extends Response<Pet> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(
    Integer status,
    Headers headers,
    Pet body
  ) implements Response2XX<Pet>, ResponsePet {
    public Response200(java.util.Optional<Integer> XRateLimit, Pet body) {
      this(200, new Headers(XRateLimit), body);
    }
    public record Headers(
    java.util.Optional<Integer> XRateLimit
  ) implements Wirespec.Response.Headers {}
  }
  record Response405(
    Integer status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response405() {
      this(405, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("pet"),
        java.util.Collections.emptyMap(),
        java.util.Collections.emptyMap(),
        java.util.Optional.ofNullable(serialization.serializeBody(request.body(), Wirespec.getType(Pet.class, null)))
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        request.body().<Pet>map(body -> serialization.deserializeBody(body, Wirespec.getType(Pet.class, null))).orElse(null)
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Map.ofEntries(java.util.Map.entry("X-Rate-Limit", serialization.<java.util.Optional<Integer>>serializeParam(r.headers().XRateLimit(), Wirespec.getType(Integer.class, java.util.Optional.class)))), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Pet.class, null)))); }
      if (response instanceof Response405 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.empty()); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
          serialization.<java.util.Optional<Integer>>deserializeParam(response.headers().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("X-Rate-Limit")).findFirst().map(java.util.Map.Entry::getValue).orElse(java.util.Collections.emptyList()), Wirespec.getType(Integer.class, java.util.Optional.class)),
          response.body().<Pet>map(body -> serialization.deserializeBody(body, Wirespec.getType(Pet.class, null))).orElse(null)
        );
        case 405 -> new Response405();
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.PostMapping("/pet")
    java.util.concurrent.CompletableFuture<Response<?>> addPet(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet"; }
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
