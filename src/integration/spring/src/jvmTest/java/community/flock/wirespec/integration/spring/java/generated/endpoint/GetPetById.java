package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;

public interface GetPetById extends Wirespec.Endpoint {
  public record Path(
    Long petId
  ) implements Wirespec.Path {}

  static class Queries implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(Long petId) {
      this(new Path(petId), Wirespec.Method.GET, new Queries(), new RequestHeaders(), null);
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
    public Response200(Pet body) {
      this(200, new Headers(), body);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
  record Response400(
    Integer status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response400() {
      this(400, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
  record Response404(
    Integer status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response404() {
      this(404, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method().name(),
        java.util.List.of("pet", serialization.serializePath(request.path().petId(), Wirespec.getType(Long.class, null))),
        java.util.Collections.emptyMap(),
        java.util.Collections.emptyMap(),
        java.util.Optional.empty()
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializePath(request.path().get(1), Wirespec.getType(Long.class, null))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Pet.class, null)))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.empty()); }
      if (response instanceof Response404 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), java.util.Optional.empty()); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
          response.body().<Pet>map(body -> serialization.deserializeBody(body, Wirespec.getType(Pet.class, null))).orElse(null)
        );
        case 400 -> new Response400();
        case 404 -> new Response404();
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.GetMapping("/pet/{petId}")
    java.util.concurrent.CompletableFuture<Response<?>> getPetById(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet/{petId}"; }
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
