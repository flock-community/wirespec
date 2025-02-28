package community.flock.wirespec.integration.spring.java.generated;

import community.flock.wirespec.java.Wirespec;

public interface FindPetsByStatusEndpoint extends Wirespec.Endpoint {
  class Path implements Wirespec.Path {}

  public record Queries(
    java.util.Optional<FindPetsByStatusParameterStatus> status
  ) implements Wirespec.Queries {}

  class RequestHeaders implements Wirespec.Request.Headers {}

  class Request implements Wirespec.Request<Void> {
    private final Path path;
    private final Wirespec.Method method;
    private final Queries queries;
    private final RequestHeaders headers;
    private final Void body;
    public Request(java.util.Optional<FindPetsByStatusParameterStatus> status) {
      this.path = new Path();
      this.method = Wirespec.Method.GET;
      this.queries = new Queries(status);
      this.headers = new RequestHeaders();
      this.body = null;
    }
    @Override public Path getPath() { return path; }
    @Override public Wirespec.Method getMethod() { return method; }
    @Override public Queries getQueries() { return queries; }
    @Override public RequestHeaders getHeaders() { return headers; }
    @Override public Void getBody() { return body; }
  }

  sealed interface Response<T> extends Wirespec.Response<T> {}
  sealed interface Response2XX<T> extends Response<T> {}
  sealed interface Response4XX<T> extends Response<T> {}
  sealed interface ResponseListPet extends Response<java.util.List<Pet>> {}
  sealed interface ResponseVoid extends Response<Void> {}

  record Response200(java.util.List<Pet> body) implements Response2XX<java.util.List<Pet>>, ResponseListPet {
    @Override public int getStatus() { return 200; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public java.util.List<Pet> getBody() { return body; }
    class Headers implements Wirespec.Response.Headers {}
  }
  record Response400() implements Response4XX<Void>, ResponseVoid {
    @Override public int getStatus() { return 400; }
    @Override public Headers getHeaders() { return new Headers(); }
    @Override public Void getBody() { return null; }
    class Headers implements Wirespec.Response.Headers {}
  }

  interface Handler extends Wirespec.Handler {

    static Wirespec.RawRequest toRequest(Wirespec.Serializer<String> serialization, Request request) {
      return new Wirespec.RawRequest(
        request.method.name(),
        java.util.List.of("pet", "findByStatus"),
        java.util.Map.ofEntries(java.util.Map.entry("status", serialization.serializeParam(request.queries.status, Wirespec.getType(FindPetsByStatusParameterStatus.class, false)))),
        java.util.Collections.emptyMap(),
        serialization.serialize(request.getBody(), Wirespec.getType(Void.class, false))
      );
    }

    static Request fromRequest(Wirespec.Deserializer<String> serialization, Wirespec.RawRequest request) {
      return new Request(
        java.util.Optional.ofNullable(request.queries().get("status")).map(it -> serialization.<FindPetsByStatusParameterStatus>deserializeParam(it, Wirespec.getType(FindPetsByStatusParameterStatus.class, false)))
      );
    }

    static Wirespec.RawResponse toResponse(Wirespec.Serializer<String> serialization, Response<?> response) {
      if (response instanceof Response200 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), serialization.serialize(r.body, Wirespec.getType(Pet.class, true))); }
      if (response instanceof Response400 r) { return new Wirespec.RawResponse(r.getStatus(), java.util.Collections.emptyMap(), null); }
      else { throw new IllegalStateException("Cannot match response with status: " + response.getStatus());}
    }

    static Response<?> fromResponse(Wirespec.Deserializer<String> serialization, Wirespec.RawResponse response) {
      return switch (response.statusCode()) {
        case 200 -> new Response200(
        serialization.deserialize(response.body(), Wirespec.getType(Pet.class, true))
      );
        case 400 -> new Response400();
        default -> throw new IllegalStateException("Cannot match response with status: " + response.statusCode());
      };
    }

    @org.springframework.web.bind.annotation.GetMapping("/pet/findByStatus")
    java.util.concurrent.CompletableFuture<Response<?>> findPetsByStatus(Request request);

    class Handlers implements Wirespec.Server<Request, Response<?>>, Wirespec.Client<Request, Response<?>> {
      @Override public String getPathTemplate() { return "/pet/findByStatus"; }
      @Override public String getMethod() { return "GET"; }
      @Override public Wirespec.ServerEdge<Request, Response<?>> getServer(Wirespec.Serialization<String> serialization) {
        return new Wirespec.ServerEdge<>() {
          @Override public Request from(Wirespec.RawRequest request) { return fromRequest(serialization, request); }
          @Override public Wirespec.RawResponse to(Response<?> response) { return toResponse(serialization, response); }
        };
      }
      @Override public Wirespec.ClientEdge<Request, Response<?>> getClient(Wirespec.Serialization<String> serialization) {
        return new Wirespec.ClientEdge<>() {
          @Override public Wirespec.RawRequest to(Request request) { return toRequest(serialization, request); }
          @Override public Response<?> from(Wirespec.RawResponse response) { return fromResponse(serialization, response); }
        };
      }
    }
  }
}
