package community.flock.wirespec.integration.spring.java.generated.endpoint;

import community.flock.wirespec.integration.spring.java.generated.model.FindPetsByStatusParameterStatus;
import community.flock.wirespec.integration.spring.java.generated.model.Pet;
import community.flock.wirespec.java.Wirespec;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;




public interface FindPetsByStatus extends Wirespec.Endpoint {
  static class Path implements Wirespec.Path {}

  public record Queries(
    Optional<FindPetsByStatusParameterStatus> status
  ) implements Wirespec.Queries {}

  static class RequestHeaders implements Wirespec.Request.Headers {}

  record Request (
    Path path,
    Wirespec.Method method,
    Queries queries,
    RequestHeaders headers,
    Void body
  ) implements Wirespec.Request<Void> {
    public Request(Optional<FindPetsByStatusParameterStatus> status) {
      this(new Path(), Wirespec.Method.GET, new Queries(status), new RequestHeaders(), null);
    }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponseListPet extends Response<List<Pet>> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(
    int status,
    Headers headers,
    List<Pet> body
  ) implements Response2XX<List<Pet>>, ResponseListPet {
    public Response200(List<Pet> body) {
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
        List.of("pet", "findByStatus"),
        Map.ofEntries(Map.entry("status", serialization.serializeParam(request.queries().status(), Wirespec.getType(FindPetsByStatusParameterStatus.class, Optional.class)))),
        Collections.emptyMap(),
        Optional.empty()
      );
    }

    static Request fromRequest(Wirespec.Deserializer serialization, Wirespec.RawRequest request) {
      return new Request(
        serialization.<Optional<FindPetsByStatusParameterStatus>>deserializeParam(request.queries().getOrDefault("status", Collections.emptyList()), Wirespec.getType(FindPetsByStatusParameterStatus.class, Optional.class))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.status(), Collections.emptyMap(), Optional.ofNullable(serialization.serializeBody(r.body, Wirespec.getType(Pet.class, List.class)))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.status(), Collections.emptyMap(), Optional.empty()); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.status());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
          response.body().<List<Pet>>map(body -> serialization.deserializeBody(body, Wirespec.getType(Pet.class, List.class))).orElse(null)
        );
        case 400 -> new Response400();
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.GetMapping("/pet/findByStatus")
    CompletableFuture<Response<?>> findPetsByStatus(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet/findByStatus"; }
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
