package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.java.Wirespec;

import community.flock.wirespec.integration.spring.java.generated.model.Pet;

public interface UpdatePet extends Wirespec.Endpoint {
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
      this(new Path(), Wirespec.Method.PUT, new Queries(), new RequestHeaders(), body);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponsePet extends Response<Pet> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(
    int status,
    Headers headers,
    Pet body
  ) implements Response2XX<Pet>, ResponsePet {
    public Response200(Pet body) {
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
  record Response404(
    int status,
    Headers headers,
    Void body
  ) implements Response4XX<Void>, ResponseVoid {
    public Response404() {
      this(404, new Headers(), null);
    }
    static class Headers implements Wirespec.Response.Headers {}
  }
  record Response405(
    int status,
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
        serialization.serializeBody(request.body(), Wirespec.getType(Pet.class, null))
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.deserializeBody(request.body(), Wirespec.getType(Pet.class, null))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), serialization.serializeBody(r.body, Wirespec.getType(Pet.class, null))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), null); }
      if (response instanceof Response404 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), null); }
      if (response instanceof Response405 r) { return new Wirespec.RawResponse(r.status(), java.util.Collections.emptyMap(), null); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
          serialization.deserializeBody(response.body(), Wirespec.getType(Pet.class, null))
        );
        case 400 -> new Response400();
        case 404 -> new Response404();
        case 405 -> new Response405();
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.PutMapping("/pet")
    java.util.concurrent.CompletableFuture<Response<?>> updatePet(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet"; }
      @Override public String getMethod() { return "PUT"; }
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
